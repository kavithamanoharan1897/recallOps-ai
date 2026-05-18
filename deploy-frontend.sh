#!/bin/bash

# RecallOps AI - Frontend-Only Deployment Script
# ---------------------------------------------

# Configuration
PROJECT_ID="recallops-ai"
REGION="us-central1"
FRONTEND_SERVICE="recallops-frontend"
REPO_NAME="recallops-repo"
BACKEND_URL="https://recallops-backend-vnxubjihba-uc.a.run.app/api"

echo "🚀 Starting Frontend-Only Deployment for RecallOps AI..."
echo "🔗 Using Backend URL: $BACKEND_URL"

# 1. Build and Push Frontend
echo "🔨 Building Frontend Image..."
gcloud builds submit ./frontend \
    --config ./frontend/cloudbuild.yaml \
    --substitutions "_API_URL=$BACKEND_URL,_REGION=$REGION,_REPO_NAME=$REPO_NAME,_SERVICE_NAME=$FRONTEND_SERVICE"

# 2. Deploy Frontend to Cloud Run
echo "☁️ Deploying Frontend to Cloud Run..."
gcloud run deploy $FRONTEND_SERVICE \
    --image $REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$FRONTEND_SERVICE:latest \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --port 80

FRONTEND_URL=$(gcloud run services describe $FRONTEND_SERVICE --region $REGION --format='value(status.url)')

echo "--------------------------------------------------"
echo "✨ Frontend Deployment Complete!"
echo "🔗 Frontend URL: $FRONTEND_URL"
echo "🔗 Backend URL: $BACKEND_URL"
echo "--------------------------------------------------"
