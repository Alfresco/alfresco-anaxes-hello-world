# Anaxes Hello World App

This Helm chart provides an example deployment of the Anaxes Hello World App.

The chart is intended to serve as an example of how a team should build, package and deploy to Kubernetes clusters using Anaxes artifacts and best practices.

This chart depends on the following charts to get the anaxes Hello World Service and anaxes Hello World UI:

* [Hello World Service](https://github.com/Alfresco/alfresco-anaxes-hello-world-service-deployment/tree/master/helm/hello-world-service)
* [Hello World UI](https://github.com/Alfresco/alfresco-anaxes-hello-world-ui-deployment/tree/master/helm/hello-world-ui)

You can deploy this chart to a Kubernetes cluster with:

    helm install hello-world-app
