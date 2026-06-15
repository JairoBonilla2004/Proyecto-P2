#!/usr/bin/env python3
"""
Script para crear artefactos dummy del modelo para testing.

Genera artefactos .joblib mínimos para permitir testing sin el modelo real.
"""

import joblib
import logging
from pathlib import Path
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.preprocessing import StandardScaler
from sklearn.feature_selection import SelectKBest, f_classif

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


def create_dummy_artifacts():
    """Crea artefactos dummy para testing."""
    
    # Script runs from ci/ so we need to go one level up to reach repo-level modelo/
    artifacts_dir = Path("../modelo/model_artifacts")
    artifacts_dir.mkdir(parents=True, exist_ok=True)

    logger.info("Creating dummy model artifacts for testing...")

    # 1. Crear modelo dummy (Random Forest)
    X_train = np.random.randn(100, 14)
    y_train = np.random.randint(0, 2, 100)

    model = RandomForestClassifier(n_estimators=10, random_state=42)
    model.fit(X_train, y_train)

    model_path = artifacts_dir / "modelo_ensemble.joblib"
    joblib.dump(model, model_path)
    logger.info(f"✅ Created: {model_path}")

    # 2. Crear vectorizer TF-IDF dummy
    texts = [
        "eval dangerous function",
        "subprocess system call",
        "safe normal code",
    ] * 20

    vectorizer = TfidfVectorizer(max_features=100)
    vectorizer.fit(texts)

    vectorizer_path = artifacts_dir / "vectorizador_tfidf.joblib"
    joblib.dump(vectorizer, vectorizer_path)
    logger.info(f"✅ Created: {vectorizer_path}")

    # 3. Crear scaler dummy
    X_scale = np.random.randn(100, 14)
    scaler = StandardScaler()
    scaler.fit(X_scale)

    scaler_path = artifacts_dir / "scaler_estructural.joblib"
    joblib.dump(scaler, scaler_path)
    logger.info(f"✅ Created: {scaler_path}")

    # 4. Crear selector de features dummy
    X_select = np.random.randn(100, 14)
    y_select = np.random.randint(0, 2, 100)

    selector = SelectKBest(f_classif, k=10)
    selector.fit(X_select, y_select)

    selector_path = artifacts_dir / "selector_features.joblib"
    joblib.dump(selector, selector_path)
    logger.info(f"✅ Created: {selector_path}")

    logger.info("\n" + "=" * 60)
    logger.info("Dummy artifacts created successfully!")
    logger.info(f"Location: {artifacts_dir}")
    logger.info(
        "\nNote: These are dummy models for testing only.\n"
        "Replace with real trained models in production."
    )
    logger.info("=" * 60)


if __name__ == "__main__":
    try:
        create_dummy_artifacts()
    except Exception as e:
        logger.error(f"Error creating artifacts: {e}")
        exit(1)
