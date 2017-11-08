# package maven service artifact
pom_location="service/pom.xml"
artifact_version=$(mvn -f $pom_location org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -Ev '(^\[|Download\w+:)' 2> /dev/null)
artifact_id=$(mvn -f $pom_location org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.artifactId | grep -Ev '(^\[|Download\w+:)' 2> /dev/null)
finalName=$artifact_id-$artifact_version'.jar'

mvn -f $pom_location clean package

# build-push service docker image
cp service/target/$finalName service-deployment/
cd service-deployment
./docker-build-push.sh $finalName $artifact_id $artifact_version
rm $finalName

build-push ui docker iomage

# cd ../
cp -R ui ui-deployment/
cd ui-deployment
./docker-build-push.sh $artifact_version
rm -rf ui/
