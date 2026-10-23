FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY run.py ./run.py
COPY confidence ./confidence
COPY models ./models
ENV CONFIDENCE_HOST=0.0.0.0
EXPOSE 8107
CMD ["python", "run.py"]
