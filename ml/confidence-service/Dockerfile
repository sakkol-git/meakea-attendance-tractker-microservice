FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY confidence ./confidence
COPY models ./models
EXPOSE 8107
CMD ["uvicorn", "confidence.api:app", "--host", "0.0.0.0", "--port", "8107"]
