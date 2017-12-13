# Hello World Example

To get familiar with how an Alfresco Engineer or a Solution Developer can build and use a deployment package for Kubernetes we have created a simple hello world app that you can use for reference as you get started.

The application consists of several components:
* Database
	* To store the data, [postgres](https://github.com/kubernetes/charts/tree/master/stable/postgresql) in our case
* [Backend REST Service](https://github.com/Alfresco/alfresco-anaxes-hello-world-service)
	* To Create/Read/Update/Delete entries in the db
* [Backend REST Service Deployment](https://github.com/Alfresco/alfresco-anaxes-hello-world-service-deployment)
	* For creating Docker images and Helm charts for the service
* [Frontend UI](https://github.com/Alfresco/alfresco-anaxes-hello-world-ui)
	* As the interface to the backend service
* [Frontend UI Deployment](https://github.com/Alfresco/alfresco-anaxes-hello-world-ui-deployment)
	* For creating Docker images and Helm charts for the UI

The components, how they are packaged and deployed is shown in the diagram below:

![Component-diagram](./diagrams/component-diagram.png "component-diagram")

The interactions between the components is shown in the following diagram:

![Sequence-diagram](./diagrams/sequence-diagram.png "sequence-diagram")

## How to Deploy

1. Create the namespace and secrets based on the instruction provided in [SECRETS.md](https://github.com/Alfresco/alfresco-anaxes-shipyard/tree/master/examples/SECRETS.md)

2. As part of the infrastucture pull the ingress chart to the namespace
```bash
helm install stable/nginx-ingress --namespace example    
```

3. Add the helm repository so that chart dependencies can be pulled:

```bash
helm repo add alfresco-test https://alfresco.github.io/charts-test/incubator
```

5. Deploy the helm chart in your namespace.

Whether you are deploying to minikube or to an AWS cluster use the command below. Keep in mind that when running on AWS the app will trigger Kubernetes to generate an Elastic Load Balancer providing access to the application and service, so you will probably have to wait a bit untill it gets created and you can access the application.

```bash
helm install hello-world-app --namespace=example
```

6. Check that the deployment worked by running the command below:

```bash
kubectl get pods --namespace example
```

You should see output something similar to below. The first time you deploy the status column will say <code>ContainerCreating</code> for a while as it needs to download the docker image. If the status column shows an error take a look at the Troubleshooting section.

<pre>
NAME                                                       READY     STATUS    RESTARTS   AGE
your-bison-hello-world-app-backend-433440179-bd31c         1/1       Running   0          37m
your-bison-hello-world-app-ui-4187005864-wl4bx             1/1       Running   0          37m
your-bison-nginx-ingress-controller-289934240-f2sh1        1/1       Running   0          37m
your-bison-nginx-ingress-default-backend-714929657-7ds77   1/1       Running   0          37m
your-bison-postgresql-400070053-8mxpw                      1/1       Running   0          37m
</pre>

## Accessing the UI

1. Run the following command to get a list of your releases:

```bash
helm ls
```

2. Run the script [get-ui-url.sh](https://github.com/Alfresco/alfresco-anaxes-hello-world-ui-deployment/tree/master/scripts/get-ui-url.sh) with the appropriate release name and namespace to get the base URL for the UI:

```bash
<code-root>/scripts/get-ui-url.sh [release] [namespace]
```

3. Navigate to the returned URL to use the UI. The screenshot below shows what you should see.

![UI](./diagrams/app-ui.png)

4. To access different keys in the db just change "welcome" to the key you've created and you should be able to see the value set for that key.
Check out the next steps to find out how you can create a new key.

## Accessing the REST API

1. Run the following command to get a list of your releases:

```bash
helm ls
```

2. Run the scripts [get-backend-url.sh](https://github.com/Alfresco/alfresco-anaxes-hello-world-service-deployment/tree/master/scripts/get-backend-url.sh) with the appropriate release name and namespace to get the base URL for the REST API:


```bash
<code-root>/scripts/get-backend-url.sh [release] [namespace]
```

3. Use the following curl command to test the REST API.

```bash
curl [url-from-step-2]/welcome
```

You should see the following output:

<pre>
{"key":"welcome","value":"Hello World!"}
</pre>

4. To create a new key through the service use the following curl:

```bash
curl -H "Content-Type: application/json" -d '{"key":"new-test-data","value":"Test 1,2,3"}' [url-from-step-2]
```

5. To access different keys in the db just change "welcome" to the key you've created and you should be able to see the value set for that key.

```bash
curl [url-from-step-2]/new-test-data
```
For more examples on using the hello service you can check the ![postman collection](https://github.com/Alfresco/alfresco-anaxes-hello-world-service/tree/master/service/src/test/postman/hello-service-test-collection.json).
This collection can also imported in the ![Postman app](https://www.getpostman.com/docs/) and used there.

## Cleaning Up

1. Run the following command to get a list of your releases:

```bash
helm ls
```

2. Run the command below with the appropriate release name to uninstall the deployment:

```bash
helm delete [release-name]
```

3. Ensure everything has been removed by running the following command:

```bash
helm status [release-name]
```

You should see the following output:

<pre>
LAST DEPLOYED: Thu Nov  2 12:16:56 2017
NAMESPACE: example
STATUS: DELETED
</pre>

4. Delete the namespace.

```bash
kubectl delete namespace example
```

## Troubleshooting

If the deployment is not working correctly list the pods by executing:

```bash
kubectl get pods --namespace example
```

If a pod is showing an error in the Status column run the following command to get more detailed information:

```bash
kubectl describe pod <pod-name> --namespace example
```

If the events indicate there is a problem fetching the docker image check that the secret created in the Deploy section contains credentials. The easiest way to do this is to use the Kubernetes dashboard as shown in the screenshot below. Click the eye icon to see the secret contents.

![Secret](./diagrams/secrets-in-dashboard.png)

To get to the dashboard if you're using minikube type <code>minikube dashboard</code>. If you're using an AWS based Kubernetes cluster, type <code>kubectl proxy</code> and then navigate to <code>http://localhost:8081/ui</code> in a browser.

If the credentials are missing check they are present in ~/.docker/config.json, especially if you're running on a Mac as the "Securely store docker logins in macOS keychain" preference maybe enabled.

If you get a response of <code>http://</code> from the <code>get-ui-url.sh</code> or <code>get-backend-url.sh</code> when deploying to a cluster on AWS, it either means the Elastic Load Balancer for the service failed to create successfully, this can sometimes be due to limits in your AWS account.
