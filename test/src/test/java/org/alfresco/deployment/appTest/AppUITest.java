/*
 * Copyright 2017 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.alfresco.deployment.appTest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AppUITest extends AppAbstract
{
    private static Log logger = LogFactory.getLog(AppUITest.class);
    
    private String uiUrl;

    @BeforeClass(alwaysRun = true)
    public void setup() throws Exception
    {
        // do common setup
        commonSetup();
        
        // get the appropriate URL
        if (isMinikubeCluster())
        {
            uiUrl = getUrlForMinikube("ingress-controller");
        }
        else
        {
            uiUrl = getUrlForAWS("ingress-controller");
        }
     // add the /hello to the base url
        StringBuffer buffer = new StringBuffer(uiUrl);
        if (!uiUrl.endsWith("/"))
        {
            buffer.append("/");
        }
        buffer.append("hello-ui/welcome");
        uiUrl = buffer.toString();
        
        logger.info("UI URL: " + uiUrl);
        
        // wait for the URL to become available
        waitForURL(uiUrl,200);
    }
    
    /**
     * Test to check the UI response is correct
     * @throws Exception
     * @throws
     */
    @Test
    public void testHelloWorldAppUrl() throws Exception
    { 
        CloseableHttpClient client = null;
        CloseableHttpResponse response= null;
        BufferedReader rd = null ;
        try
        {
            client = HttpClientBuilder.create().build();
            HttpGet getRequest = new HttpGet(uiUrl);
            response = client.execute(getRequest);
            rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
    
            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null)
            {
                result.append(line);
            }
            String htmlOutput = result.toString();
            
            Assert.assertFalse(htmlOutput.contains("error"), String.format("The page is not loaded correctly it contains error [%s]", htmlOutput));
            Assert.assertTrue(htmlOutput.contains("<title>Demo Application</title>"), String.format("The title is not displayed correctly and the result is [%s]",htmlOutput));
        }
        finally
        {
            if (rd != null) rd.close();
            if (response != null) response.close();
            if (client != null) client.close();
        }
    }
    
    /**
     * Test to validate the UI dom is displayed correctly
     * This test will create a selenium remote driver and validate that
     * UI display the correct content
     */
    @Test
    public void testHelloWorldUI() throws Exception
    {
        // I have defaulted to standalone container selenium hub
        RemoteWebDriver driver = null;
        try
        {
            driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), DesiredCapabilities.firefox());
            driver.navigate().to(uiUrl);
            Assert.assertTrue(driver.getTitle().contains("Demo Application"),
                    String.format("The title is not displayed correctly and the result is [%s]", driver.getTitle()));
            // Added a wait of 5 seconds to get the dom full populated.
            Thread.sleep(5000);
           Assert.assertTrue(driver.findElement(By.tagName("body")).getText().contains("Hello World!"),
                    String.format("The dom source does not contain'Hello World!'"));
        }
        finally
        {
            if (driver != null)
                driver.quit();
        }
    }
}
