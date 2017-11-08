final_name=$1
artifact_id=$2
tag=$3
docker build --build-arg final_name=$final_name -t quay.io/alfresco/$artifact_id:$tag .
docker push quay.io/alfresco/$artifact_id:$tag
