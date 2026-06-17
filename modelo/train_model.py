#!/usr/bin/env python3
import json
import logging
import math
import os
import re
import sys
import time
import warnings
from datetime import datetime
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
import xgboost as xgb
from imblearn.over_sampling import SMOTE
from imblearn.pipeline import Pipeline as ImbPipeline
from scipy.sparse import csr_matrix, hstack
from sklearn.ensemble import RandomForestClassifier, VotingClassifier
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.feature_selection import SelectKBest, chi2
from sklearn.metrics import classification_report, confusion_matrix
from sklearn.model_selection import (
    GridSearchCV,
    StratifiedKFold,
    cross_validate,
    train_test_split,
)
from sklearn.preprocessing import MinMaxScaler

warnings.filterwarnings("ignore")

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "ci"))
from feature_extractor import extraer_features_codigo  # noqa: E402

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

RUTA_DATASET = Path(__file__).parent / "juliet_java_dataset.csv"
RUTA_ARTEFACTOS = Path(__file__).parent / "model_artifacts"
SEED = 42
CV_FOLDS = 5
UMBRAL = 0.82


def cargar_dataset(ruta):
    print("=" * 60)
    print("CARGA DEL DATASET JULIET TEST SUITE")
    print("=" * 60)
    df = pd.read_csv(ruta)
    print(f"Registros: {len(df)} | CWEs: {df['cwe'].nunique()}")
    print(f"Labels:\n{df['label'].value_counts()}")
    print(f"Longitud codigo promedio: {df['code'].str.len().mean():.0f} chars")
    df = df.dropna(subset=["code", "label"]).reset_index(drop=True)
    df["code"] = df["code"].astype(str)
    df["label"] = df["label"].astype(int)
    return df


def extraer_features(df):
    print("\n" + "=" * 60)
    print("EXTRACCION DE FEATURES ESTRUCTURALES")
    print("=" * 60)
    feats = []
    total = len(df)
    for i, cod in enumerate(df["code"]):
        if (i + 1) % 4000 == 0:
            print(f"  {i+1}/{total}...")
        feats.append(extraer_features_codigo(cod))
    df_f = pd.DataFrame(feats)
    print(f"Features: {len(df_f.columns)}")
    print(f"Columnas: {list(df_f.columns)}")
    print("\nPromedio por clase:")
    cols = [c for c in df_f.columns if c in
            ["total_danger_score", "total_safety_score", "ast_depth",
             "dangerous_func_calls", "sql_raw_concat", "sanitization_present",
             "lines_count", "cyclomatic_complexity", "exception_handling"]]
    print(pd.concat([df_f, df["label"]], axis=1).groupby("label")[cols].mean().round(3))
    return df_f


def vectorizar(df, df_f, y):
    print("\n" + "=" * 60)
    print("TF-IDF + FUSION")
    print("=" * 60)
    token_pat = (
        r"(?:"
        r"[a-zA-Z_$][\w$]*"
        r"|\d+\.?\d*"
        r"|==|!=|<=|>=|\+=|-=|\*=|/=|->|:="
        r"|[()\[\]{}=+\-*/%<>!&|^~@,.:;]"
        r")"
    )
    vec = TfidfVectorizer(token_pattern=token_pat, ngram_range=(1, 3), min_df=2,
                           max_df=0.95, max_features=8000, sublinear_tf=True,
                           strip_accents="unicode", analyzer="word")
    X_tfidf = vec.fit_transform(df["code"])
    print(f"TF-IDF: {X_tfidf.shape} (sparsity: {1.0 - X_tfidf.nnz / (X_tfidf.shape[0] * X_tfidf.shape[1]):.4f})")

    scaler = MinMaxScaler()
    X_struct = csr_matrix(scaler.fit_transform(df_f.values.astype(float)))
    print(f"Estructurales: {X_struct.shape[1]} features")

    X_c = hstack([X_tfidf, X_struct])
    print(f"Combinada: {X_c.shape}")

    # Tokens mas discriminativos
    tokens = vec.get_feature_names_out()
    tv = np.asarray(X_tfidf[y == 1].mean(axis=0)).flatten()
    ts = np.asarray(X_tfidf[y == 0].mean(axis=0)).flatten()
    diff = tv - ts
    print("\nTop 10 tokens VULNERABLE:")
    for i in np.argsort(diff)[::-1][:10]:
        print(f"  {tokens[i]}: {diff[i]:.4f}")
    print("Top 10 tokens SEGURO:")
    for i in np.argsort(diff)[:10]:
        print(f"  {tokens[i]}: {diff[i]:.4f}")

    return X_c, vec, scaler


def grid_search(X, y, nombre, pipeline, param_grid):
    print(f"\n{'=' * 60}")
    print(f"GRID SEARCH: {nombre}")
    print(f"{'=' * 60}")
    gs = GridSearchCV(pipeline, param_grid, cv=StratifiedKFold(n_splits=3, shuffle=True, random_state=SEED),
                      scoring="accuracy", n_jobs=-1, verbose=1)
    gs.fit(X, y)
    print(f"\nMejores params: {gs.best_params_}")
    print(f"Mejor accuracy: {gs.best_score_:.4f}")
    return gs.best_estimator_, gs.best_params_


def evaluar(modelo, X, y, nombre="Modelo"):
    print(f"\n{'=' * 60}")
    print(f"VALIDACION CRUZADA {CV_FOLDS}-FOLD: {nombre}")
    print(f"{'=' * 60}")
    cv = StratifiedKFold(n_splits=CV_FOLDS, shuffle=True, random_state=SEED)
    t0 = time.time()
    res = cross_validate(modelo, X, y, cv=cv,
                         scoring={"accuracy": "accuracy", "f1": "f1", "precision": "precision",
                                  "recall": "recall", "roc_auc": "roc_auc"},
                         n_jobs=-1)
    elapsed = time.time() - t0
    m = {k: float(res[f"test_{k}"].mean()) for k in ["accuracy", "f1", "precision", "recall", "roc_auc"]}
    m["accuracy_std"] = float(res["test_accuracy"].std())
    m["accuracy_por_pliegue"] = [round(float(v), 4) for v in res["test_accuracy"]]
    m["cumple"] = m["accuracy"] >= UMBRAL
    print(f"  Accuracy:  {m['accuracy']:.4f} +- {m['accuracy_std']:.4f}")
    print(f"  Detalle:   {m['accuracy_por_pliegue']}")
    print(f"  F1:        {m['f1']:.4f}")
    print(f"  Precision: {m['precision']:.4f}")
    print(f"  Recall:    {m['recall']:.4f}")
    print(f"  AUC-ROC:   {m['roc_auc']:.4f}")
    print(f"  Tiempo:    {elapsed:.1f}s")
    print(f"  Estado:    {'APRUEBA' if m['cumple'] else 'NO APRUEBA'}")
    return m


def reporte_final(pipeline, X, y):
    print("\n" + "=" * 60)
    print("REPORTE DETALLADO - TEST SPLIT 80/20")
    print("=" * 60)
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=SEED, stratify=y)
    pipeline.fit(X_tr, y_tr)
    y_pred = pipeline.predict(X_te)

    print("\nClassification Report:")
    print(classification_report(y_te, y_pred, target_names=["SEGURO", "VULNERABLE"], digits=4))

    cm = confusion_matrix(y_te, y_pred)
    print("Matriz de confusion:")
    print(f"               Pred SEGURO  Pred VULN")
    print(f"Real SEGURO     {cm[0][0]:>5}       {cm[0][1]:>5}")
    print(f"Real VULN       {cm[1][0]:>5}       {cm[1][1]:>5}")
    print(f"\nFP (falsas alarmas): {cm[0][1]}")
    print(f"FN (vulnerabilidades no detectadas): {cm[1][0]}")

    # Devolver el ensemble entrenado (dentro del pipeline)
    return pipeline.named_steps["ensemble"]


def exportar(modelo, vec, scaler, selector, X_text, y, metricas):
    print("\n" + "=" * 60)
    print("EXPORTACION DE ARTEFACTOS")
    print("=" * 60)
    RUTA_ARTEFACTOS.mkdir(parents=True, exist_ok=True)

    artefactos = {
        "modelo_ensemble": modelo,
        "vectorizador_tfidf": vec,
        "selector_features": selector,
        "scaler_estructural": scaler,
    }
    rutas = {}
    for nom, obj in artefactos.items():
        r = RUTA_ARTEFACTOS / f"{nom}.joblib"
        joblib.dump(obj, r, compress=3)
        rutas[nom] = str(r)
        print(f"  {nom}.joblib ({os.path.getsize(r) / 1024:.1f} KB)")

    meta = {
        "nombre_modelo": "Juliet_Java_VulnerabilityClassifier_v2",
        "version": "2.0.0",
        "fecha_entrenamiento": datetime.now().isoformat(),
        "dataset": "Juliet Test Suite for Java v1.3",
        "tipo_modelo": "Ensemble (RandomForest + XGBoost, VotingSoft)",
        "total_muestras": int(len(X_text)),
        "balanceo": "SMOTE via ImbPipeline (sin leakage)",
        "validacion_cruzada": {
            "cv_folds": CV_FOLDS,
            "umbral_requerido": UMBRAL,
            "accuracy_mean": round(metricas["accuracy"], 4),
            "accuracy_std": round(metricas["accuracy_std"], 4),
            "f1_mean": round(metricas["f1"], 4),
            "precision_mean": round(metricas["precision"], 4),
            "recall_mean": round(metricas["recall"], 4),
            "roc_auc_mean": round(metricas["roc_auc"], 4),
            "cumple_umbral": metricas["cumple"],
            "accuracy_por_pliegue": metricas["accuracy_por_pliegue"],
        },
        "features": {
            "tipo": "TF-IDF + Estructurales (29 features)",
            "tfidf_max_features": 8000,
            "ngram_range": [1, 3],
            "feature_selection": "SelectKBest(chi2)",
        },
        "artefactos": list(rutas.keys()),
    }
    p = RUTA_ARTEFACTOS / "model_metadata.json"
    with open(p, "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2, ensure_ascii=False)
    print(f"\n  metadata.json ({os.path.getsize(p) / 1024:.1f} KB)")

    print(f"\nAccuracy: {metricas['accuracy']:.4f} "
          f"({'APRUEBA' if metricas['cumple'] else 'NO APRUEBA'})")
    print(f"Exportado a: {RUTA_ARTEFACTOS.resolve()}")


def main():
    print("=" * 60)
    print("ENTRENAMIENTO: Clasificador Vulnerabilidades Java")
    print(f"Juliet Test Suite | {datetime.now():%Y-%m-%d %H:%M}")
    print("=" * 60)

    df = cargar_dataset(RUTA_DATASET)
    y = df["label"].values

    df_f = extraer_features(df)

    X_c, vec, scaler = vectorizar(df, df_f, y)

    # GridSearch para RandomForest
    pipe_rf = ImbPipeline([
        ("select", SelectKBest(chi2)),
        ("smote", SMOTE(random_state=SEED)),
        ("clf", RandomForestClassifier(random_state=SEED, n_jobs=-1, class_weight="balanced")),
    ])
    _, best_rf_params = grid_search(X_c, y, "RandomForest", pipe_rf, {
        "select__k": [2000, 3000],
        "clf__n_estimators": [100, 200],
        "clf__max_depth": [10, 20],
    })

    # GridSearch para XGBoost
    pipe_xgb = ImbPipeline([
        ("select", SelectKBest(chi2)),
        ("smote", SMOTE(random_state=SEED)),
        ("clf", xgb.XGBClassifier(random_state=SEED, n_jobs=-1,
                                    eval_metric="logloss", tree_method="hist")),
    ])
    _, best_xgb_params = grid_search(X_c, y, "XGBoost", pipe_xgb, {
        "select__k": [2000, 3000],
        "clf__n_estimators": [100, 200],
        "clf__max_depth": [4, 6],
        "clf__learning_rate": [0.1, 0.2],
    })

    best_k = max(best_rf_params.get("select__k", 3000), best_xgb_params.get("select__k", 3000))
    selector = SelectKBest(chi2, k=best_k)

    rf = RandomForestClassifier(
        n_estimators=best_rf_params.get("clf__n_estimators", 200),
        max_depth=best_rf_params.get("clf__max_depth", 20),
        min_samples_split=5, min_samples_leaf=2, max_features="sqrt",
        class_weight="balanced", random_state=SEED, n_jobs=-1, oob_score=True,
    )
    xgb_clf = xgb.XGBClassifier(
        n_estimators=best_xgb_params.get("clf__n_estimators", 200),
        max_depth=best_xgb_params.get("clf__max_depth", 6),
        learning_rate=best_xgb_params.get("clf__learning_rate", 0.1),
        subsample=0.8, colsample_bytree=0.8, reg_alpha=0.1, reg_lambda=1.0,
        eval_metric="logloss", random_state=SEED, n_jobs=-1, tree_method="hist",
    )
    ensemble = VotingClassifier(
        estimators=[("rf", rf), ("xgb", xgb_clf)],
        voting="soft", weights=[1, 1], n_jobs=-1,
    )

    pipeline = ImbPipeline([
        ("select", selector),
        ("smote", SMOTE(random_state=SEED, k_neighbors=5)),
        ("ensemble", ensemble),
    ])

    metricas = evaluar(pipeline, X_c, y, "Ensemble RF+XGB")

    modelo_final = reporte_final(pipeline, X_c, y)
    selector_final = pipeline.named_steps["select"]

    exportar(modelo_final, vec, scaler, selector_final, df["code"], y, metricas)

    print("\n" + "=" * 60)
    print("ENTRENAMIENTO COMPLETADO")
    print("=" * 60)


if __name__ == "__main__":
    main()
