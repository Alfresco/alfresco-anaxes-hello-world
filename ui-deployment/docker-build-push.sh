tag=$1
artifact_id="anaxes-hello-world-ui"
docker build -t quay.io/alfresco/$artifact_id:$tag .
docker push quay.io/alfresco/$artifact_id:$tag
