/*
 * Copyright 2018 Alfresco Software, Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.alfresco.deployment.appTest;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class AppAbstract
{
    private static final String CLUSTER_TYPE = "cluster.type";
    private static final String CLUSTER_NAMESPACE = "cluster.namespace";
    private static final String URL = "restapi.url";
    private static Log logger = LogFactory.getLog(AppAbstract.class);

    private String clusterType;
    private String clusterNamespace;
    protected String url;
    private boolean isMinikubeCluster = false;
    private Properties appProperty = new Properties();
    private KubernetesClient client = new DefaultKubernetesClient();
    private final int RETRY_COUNT = 20;
    private final long SLEEP_DURATION = 15000;

    /**
     * Perform common setup, determines cluster type and namespace
     */
    public void commonSetup() throws Exception
    {
        // load properties file
        appProperty.load(this.getClass().getClassLoader().getResourceAsStream("test.properties"));

        // get cluster type, first check system property, fall back to properties file
        // first get the url property if it set .. then do not bother to go further
        url = System.getProperty(URL);
        if(url==null)
        {
        	url = appProperty.getProperty(URL);
        }
        //** if url is empty or null only do it via cluster type and cluster name space .. 
        if(url == null || url.isEmpty())
        {
            logger.info("No rest URL provided falling baking to cluster type and name.");
            clusterType = System.getProperty(CLUSTER_TYPE);
        if (clusterType == null)
        {
            clusterType = appProperty.getProperty(CLUSTER_TYPE);
        }

        // get cluster namespace, first check system property, fall back to properties file
        clusterNamespace = System.getProperty(CLUSTER_NAMESPACE);
        if (clusterNamespace == null)
        {
            clusterNamespace = appProperty.getProperty(CLUSTER_NAMESPACE);
        }

        logger.info("clusterType: " + clusterType);
        logger.info("clusterNamespace: " + clusterNamespace);

        if (clusterNamespace == null || clusterNamespace.isEmpty())
        {
            throw new IllegalStateException("Cluster namespace is required, set namespace details in system property or properties file");
        }

        // ensure namespace is lower case
        clusterNamespace = clusterNamespace.toLowerCase();

        // set cluster type flag
        if (clusterType == null || clusterType.isEmpty() || "minikube".equalsIgnoreCase(clusterType))
        {
            isMinikubeCluster = true;
        }
        }
    }

    /**
     * Determines whether the cluster type is minikube
     *
     * @return true if the cluster under test is minikube, false otherwise
     */
    protected boolean isMinikubeCluster()
    {
        return isMinikubeCluster;
    }

    /**
     * Finds a service url running in minikube. This is a generic method
     * based on the service type it can find the node port.
     */
    protected String getUrlForMinikube(String serviceType) throws Exception
    {
        String clusterUrl = client.getMasterUrl().toString();
        logger.info("cluster URL: " + clusterUrl);

        int nodePort = -1;
        int i = 0;
        long sleepTotal = 0;
        while ((i <= RETRY_COUNT) & (nodePort == -1))
        {
            // find the port number for the given 'runType'
            List<Service> services = client.services().inNamespace(clusterNamespace).list().getItems();
            logger.info("Found " + services.size() + " services");
            for (Service service : services)
            {
                if (service.getMetadata().getName().contains(serviceType))
                {
                    logger.info("Looking up nodePort for service: " + service.getMetadata().getName());
                    if (service.getSpec().getPorts().size() != 0)
                    {
                        nodePort = service.getSpec().getPorts().get(0).getNodePort();
                        break;
                    }
                }
            }

            // try again if url was not found
            if (nodePort == -1)
            {
                logger.info("nodePort is not available, sleeping for " + (SLEEP_DURATION/1000) + " seconds, retry count: " + i);
                Thread.sleep(SLEEP_DURATION);
                i++;
                sleepTotal = sleepTotal + SLEEP_DURATION;
            }
        }

        if (nodePort != -1)
        {
            return clusterUrl.replace("https", "http").replace("8443", Integer.toString(nodePort));
        }
        else
        {
            throw new IllegalStateException("Failed to find nodePort for runType '" + serviceType +
                        "' in namespace '" + clusterNamespace + "' after " + sleepTotal + " seconds");
        }
    }

    /**
     * Waits for the given URL to become available, unless the timeout period is reached,
     * in which case an exception is thrown.
     *
     * @throws IllegalStateException
     */
    protected void waitForURL(String url, int statusCode) throws Exception
    {
        logger.info("Waiting for '" + url + "' to become available...");

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;
        int TIMEOUT = 6000;
        int i = 0;
        while (i <= RETRY_COUNT)
        {
            try
            {
                HttpGet getRequest = new HttpGet(url);
                RequestConfig config = RequestConfig.custom()
                            .setSocketTimeout(TIMEOUT)
                            .setConnectionRequestTimeout(TIMEOUT)
                            .setConnectTimeout(TIMEOUT).build();
                getRequest.setConfig(config);
                response = httpClient.execute(getRequest);
                logger.info("response code " + response.getStatusLine().getStatusCode());
                if (response.getStatusLine().getStatusCode() == statusCode)
                {
                    // any response here means the URL is accessible
                    logger.info("URL is available, took " + i + " retries");
                    break;
                }
                else
                {
                    if (response != null) response.close();
                    logger.info("URL is available but does not match the status code (" + response.getStatusLine().getStatusCode() +"), sleeping for " + (SLEEP_DURATION/1000) + " seconds, retry count: " + i);
                    Thread.sleep(SLEEP_DURATION);
                    i++;
                }
            }
            catch (ConnectException|UnknownHostException|SocketTimeoutException ex)
            {
                if (response != null) response.close();
                logger.info("URL is not available, sleeping for " + (SLEEP_DURATION/1000) + " seconds, retry count: " + i);
                Thread.sleep(SLEEP_DURATION);
                i++;
            }
        }

        // close the http client
        httpClient.close();

        if (i > RETRY_COUNT)
        {
            throw new IllegalStateException("URL '" + url + "' is not available");
        }
    }

}
