# package maven service artifact

output=$(printf 'ARTIFACT_VERSION=${project.version}\nARTIFACT_ID=${project.artifactId}\n0\n' | mvn -f service/pom.xml help:evaluate | grep -Ev '(^\[|Download\w+:)' 2> /dev/null)
artifact_id=$(echo "$output" | grep '^ARTIFACT_ID' | cut -d = -f 2)
artifact_version=$(echo "$output" | grep '^ARTIFACT_VERSION' | cut -d = -f 2)

finalName=$artifact_id-$artifact_version'.jar'

mvn -f service/pom.xml clean package

# build-push service docker image
cp service/target/$finalName service-deployment/
cd service-deployment
./docker-build-push.sh $finalName $artifact_id $artifact_version
rm $finalName

#build-push ui docker iomage

cd ../
cp -R ui ui-deployment/
cd ui-deployment
./docker-build-push.sh $artifact_version
rm -rf ui/
