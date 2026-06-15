.PHONY: help setup install verify test run clean lint

# Variables
PYTHON := python3
PIP := pip3
PROJECT_DIR := $(shell pwd)
CI_DIR := $(PROJECT_DIR)/ci
VENV := venv

help:
	@echo "Security Pipeline - Available Commands"
	@echo "========================================"
	@echo ""
	@echo "Setup & Installation:"
	@echo "  make setup          - Install dependencies and setup environment"
	@echo "  make install        - Install Python dependencies only"
	@echo "  make verify         - Verify setup configuration"
	@echo ""
	@echo "Development:"
	@echo "  make test           - Run unit tests"
	@echo "  make lint           - Run code quality checks"
	@echo "  make format         - Format code with black"
	@echo ""
	@echo "Execution:"
	@echo "  make run            - Run security pipeline"
	@echo "  make run-local      - Run against local changes"
	@echo ""
	@echo "Maintenance:"
	@echo "  make clean          - Clean generated files"
	@echo "  make logs           - Show pipeline logs"
	@echo ""

setup: install verify
	@echo "✅ Setup completed successfully"

install:
	@echo "Installing dependencies..."
	cd $(CI_DIR) && $(PIP) install -r requirements.txt
	@echo "✅ Dependencies installed"

verify:
	@echo "Verifying setup..."
	cd $(CI_DIR) && $(PYTHON) setup_verification.py

test:
	@echo "Running unit tests..."
	cd $(CI_DIR) && $(PYTHON) tests.py
	@echo "✅ Tests completed"

lint:
	@echo "Running code quality checks..."
	cd $(CI_DIR) && \
		$(PYTHON) -m pylint *.py --disable=all --enable=E,F 2>/dev/null || true && \
		$(PYTHON) -m black --check *.py 2>/dev/null || true && \
		$(PYTHON) -m mypy --ignore-missing-imports *.py 2>/dev/null || true
	@echo "✅ Lint checks completed"

format:
	@echo "Formatting code with black..."
	cd $(CI_DIR) && $(PYTHON) -m black *.py
	@echo "✅ Code formatted"

run:
	@echo "Running security pipeline..."
	cd $(CI_DIR) && $(PYTHON) security_pipeline.py
	@echo "✅ Pipeline execution completed"

run-local:
	@echo "Running pipeline against local changes..."
	cd $(CI_DIR) && \
		BASE_BRANCH=main \
		HEAD_BRANCH=HEAD \
		$(PYTHON) security_pipeline.py

clean:
	@echo "Cleaning up..."
	find $(CI_DIR) -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	find $(CI_DIR) -type f -name "*.pyc" -delete
	find $(CI_DIR) -type f -name ".mypy_cache" -exec rm -rf {} + 2>/dev/null || true
	rm -f $(CI_DIR)/*.log
	rm -rf $(CI_DIR)/.pytest_cache
	@echo "✅ Cleanup completed"

logs:
	@if [ -f $(CI_DIR)/security_pipeline.log ]; then \
		tail -100 $(CI_DIR)/security_pipeline.log; \
	else \
		echo "No logs found"; \
	fi

report:
	@if [ -f reports/security_report.json ]; then \
		echo "Latest Security Report:"; \
		cat reports/security_report.json | $(PYTHON) -m json.tool; \
	else \
		echo "No report found"; \
	fi

.DEFAULT_GOAL := help
