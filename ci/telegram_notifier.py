#!/usr/bin/env python3
"""
Módulo para notificaciones por Telegram.

Envía notificaciones de seguridad a Telegram usando credenciales
de variables de entorno.
"""

import logging
import os
from datetime import datetime
from typing import Optional

import requests

logger = logging.getLogger(__name__)


class TelegramNotifier:
    """Notificador por Telegram."""

    TELEGRAM_API_URL = "https://api.telegram.org/bot{token}/sendMessage"

    def __init__(
        self,
        bot_token: Optional[str] = None,
        chat_id: Optional[str] = None,
    ):
        """
        Inicializa el notificador.

        Args:
            bot_token: Token del bot (si no se proporciona, usa env var)
            chat_id: ID del chat (si no se proporciona, usa env var)

        Raises:
            ValueError: Si no se encuentran credenciales
        """
        self.bot_token = bot_token or os.getenv("TELEGRAM_BOT_TOKEN")
        self.chat_id = chat_id or os.getenv("TELEGRAM_CHAT_ID")

        if not self.bot_token:
            raise ValueError(
                "TELEGRAM_BOT_TOKEN not provided or found in environment"
            )
        if not self.chat_id:
            raise ValueError(
                "TELEGRAM_CHAT_ID not provided or found in environment"
            )

        logger.info("TelegramNotifier initialized successfully")

    def send_message(self, message: str, parse_mode: str = "HTML") -> bool:
        """
        Envía un mensaje a Telegram.

        Args:
            message: Texto del mensaje
            parse_mode: Modo de parsing (HTML, Markdown, MarkdownV2)

        Returns:
            True si se envió exitosamente, False en caso contrario
        """
        try:
            url = self.TELEGRAM_API_URL.format(token=self.bot_token)

            payload = {
                "chat_id": self.chat_id,
                "text": message,
                "parse_mode": parse_mode,
            }

            response = requests.post(
                url,
                json=payload,
                timeout=10,
            )

            if response.status_code == 200:
                logger.info("Message sent successfully to Telegram")
                return True
            else:
                logger.error(
                    f"Failed to send message. Status: {response.status_code}"
                )
                logger.error(f"Response: {response.text}")
                return False

        except requests.RequestException as e:
            logger.error(f"Request error sending message: {e}")
            return False
        except Exception as e:
            logger.error(f"Unexpected error sending message: {e}")
            return False

    def notify_pr_scan_started(
        self,
        pr_number: int,
        repository: str,
        branch: str,
    ) -> bool:
        """
        Notifica inicio de escaneo de PR.

        Args:
            pr_number: Número del PR
            repository: Nombre del repositorio
            branch: Rama analizada

        Returns:
            True si se envió exitosamente
        """
        message = (
            f"🔍 <b>Escaneo de Seguridad Iniciado</b>\n\n"
            f"<b>Repositorio:</b> {repository}\n"
            f"<b>PR:</b> #{pr_number}\n"
            f"<b>Rama:</b> {branch}\n"
            f"<b>Hora:</b> {datetime.utcnow().isoformat()}Z"
        )

        return self.send_message(message)

    def notify_pr_scan_completed(
        self,
        pr_number: int,
        repository: str,
        safe_count: int,
        vulnerable_count: int,
        overall_decision: str,
        risk_percentage: float,
    ) -> bool:
        """
        Notifica completación de escaneo.

        Args:
            pr_number: Número del PR
            repository: Nombre del repositorio
            safe_count: Cantidad de fragmentos seguros
            vulnerable_count: Cantidad de fragmentos vulnerables
            overall_decision: ACEPTAR o RECHAZAR
            risk_percentage: Porcentaje de riesgo

        Returns:
            True si se envió exitosamente
        """
        # Determinar emoji y color según decisión
        if overall_decision == "RECHAZAR":
            emoji = "🚫"
            color = "⚠️"
        else:
            emoji = "✅"
            color = "✓"

        message = (
            f"{emoji} <b>Escaneo de Seguridad Completado</b>\n\n"
            f"<b>Repositorio:</b> {repository}\n"
            f"<b>PR:</b> #{pr_number}\n"
            f"<b>Decisión:</b> {overall_decision} {color}\n\n"
            f"<b>Resultados:</b>\n"
            f"  • Fragmentos seguros: {safe_count}\n"
            f"  • Fragmentos vulnerables: {vulnerable_count}\n"
            f"  • Riesgo general: {risk_percentage:.1f}%\n\n"
            f"<b>Hora:</b> {datetime.utcnow().isoformat()}Z"
        )

        return self.send_message(message)

    def notify_error(
        self,
        pr_number: int,
        repository: str,
        error_message: str,
    ) -> bool:
        """
        Notifica error en el escaneo.

        Args:
            pr_number: Número del PR
            repository: Nombre del repositorio
            error_message: Mensaje de error

        Returns:
            True si se envió exitosamente
        """
        message = (
            f"❌ <b>Error en Escaneo de Seguridad</b>\n\n"
            f"<b>Repositorio:</b> {repository}\n"
            f"<b>PR:</b> #{pr_number}\n"
            f"<b>Error:</b> {error_message}\n\n"
            f"<b>Hora:</b> {datetime.utcnow().isoformat()}Z"
        )

        return self.send_message(message)


def main():
    """Función principal para testing."""
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )

    try:
        notifier = TelegramNotifier()

        # Mensaje de prueba
        success = notifier.notify_pr_scan_started(
            pr_number=42,
            repository="MyProject/Backend",
            branch="develop",
        )

        if success:
            print("✓ Test message sent successfully")
        else:
            print("✗ Failed to send test message")
            print("Ensure TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID are set")

    except ValueError as e:
        logger.error(f"Configuration error: {e}")
        print(f"Error: {e}")
        print("\nRequired environment variables:")
        print("  - TELEGRAM_BOT_TOKEN")
        print("  - TELEGRAM_CHAT_ID")
    except Exception as e:
        logger.error(f"Error: {e}")


if __name__ == "__main__":
    main()
