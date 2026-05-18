#!/bin/bash

# RecallOps AI - Secure Cloud Run Deployment Script
# ------------------------------------------------

# Configuration
PROJECT_ID="recallops-ai"
REGION="us-central1"
BACKEND_SERVICE="recallops-backend"
REPO_NAME="recallops-repo"

echo "🚀 Starting Secure Deployment for RecallOps AI..."

# 1. Enable Required APIs
echo "🔧 Enabling Google Cloud APIs..."
gcloud services enable artifactregistry.googleapis.com \
                       run.googleapis.com \
                       aiplatform.googleapis.com \
                       cloudbuild.googleapis.com \
                       secretmanager.googleapis.com

# 2. Create Artifact Registry if it doesn't exist
echo "📦 Creating Artifact Registry..."
gcloud artifacts repositories create $REPO_NAME \
    --repository-format=docker \
    --location=$REGION \
    --description="Docker repository for RecallOps AI" 2>/dev/null || true

# 3. Build and Push Backend
echo "🔨 Building Backend Image..."
gcloud builds submit ./backend \
    --tag $REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$BACKEND_SERVICE:latest

# 4. Deploy Backend to Cloud Run
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

# Deploy with higher resources and timeout for Java 25
if ! gcloud run deploy $BACKEND_SERVICE \
    --image $REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$BACKEND_SERVICE:latest \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --cpu 4 \
    --memory 4Gi \
    --timeout 600 \
    --set-env-vars "GOOGLE_CLOUD_PROJECT=$PROJECT_ID,GOOGLE_CLOUD_LOCATION=$REGION,SPRING_THREADS_VIRTUAL_ENABLED=true" \
    --set-secrets "GITHUB_TOKEN=GITHUB_TOKEN:latest,REASONING_ENGINE_ID=REASONING_ENGINE_ID:latest"; then
    echo "❌ Error: Backend deployment failed. Check logs for details."
    exit 1
fi

# Get Backend URL
BACKEND_URL=$(gcloud run services describe $BACKEND_SERVICE --region $REGION --format='value(status.url)' 2>/dev/null)
if [ -z "$BACKEND_URL" ]; then
    echo "❌ Error: Backend deployment failed. Aborting."
    exit 1
fi
BACKEND_URL="${BACKEND_URL}/api"

echo "✅ Backend deployed at: $BACKEND_URL"

# 5. Build and Deploy Frontend
# Handled by the specialized frontend deployment script to ensure consistency
if [ -f "./deploy-frontend.sh" ]; then
    echo "⏭️ Transitioning to Frontend Deployment..."
    # Update the URL in the frontend script for consistency
    sed -i '' "s|BACKEND_URL=.*|BACKEND_URL=\"$BACKEND_URL\"|" ./deploy-frontend.sh
    chmod +x ./deploy-frontend.sh
    ./deploy-frontend.sh
else
    echo "❌ Error: deploy-frontend.sh not found. Skipping frontend deployment."
    exit 1
fi
