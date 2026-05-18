#!/bin/bash

# RecallOps AI - Backend-Only Deployment Script
# --------------------------------------------

# Configuration
PROJECT_ID="recallops-ai"
REGION="us-central1"
BACKEND_SERVICE="recallops-backend"
REPO_NAME="recallops-repo"

echo "🚀 Starting Backend-Only Deployment for RecallOps AI..."

# 1. Build and Push Backend
echo "🔨 Building Backend Image..."
gcloud builds submit ./backend \
    --tag $REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$BACKEND_SERVICE:latest

# 2. Deploy Backend to Cloud Run
echo "☁️ Deploying Backend to Cloud Run..."
PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format='value(projectNumber)')
SERVICE_ACCOUNT="$PROJECT_NUMBER-compute@developer.gserviceaccount.com"

# Grant permission to the service account to read the secrets
gcloud secrets add-iam-policy-binding GITHUB_TOKEN \
    --member="serviceAccount:$SERVICE_ACCOUNT" \
    --role="roles/secretmanager.secretAccessor" &>/dev/null

gcloud secrets add-iam-policy-binding REASONING_ENGINE_ID \
    --member="serviceAccount:$SERVICE_ACCOUNT" \
    --role="roles/secretmanager.secretAccessor" &>/dev/null

# Deploy with high resources and timeout to ensure Java 25 / Spring AI starts reliably
gcloud run deploy $BACKEND_SERVICE \
    --image $REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$BACKEND_SERVICE:latest \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --cpu 4 \
    --memory 4Gi \
    --timeout 600 \
    --set-env-vars "GOOGLE_CLOUD_PROJECT=$PROJECT_ID,GOOGLE_CLOUD_LOCATION=$REGION,SPRING_THREADS_VIRTUAL_ENABLED=true" \
    --set-secrets "GITHUB_TOKEN=GITHUB_TOKEN:latest,REASONING_ENGINE_ID=REASONING_ENGINE_ID:latest"

BACKEND_URL=$(gcloud run services describe $BACKEND_SERVICE --region $REGION --format='value(status.url)')

echo "--------------------------------------------------"
echo "✨ Backend Deployment Complete!"
echo "🔗 Backend URL: $BACKEND_URL/api"
echo "--------------------------------------------------"
