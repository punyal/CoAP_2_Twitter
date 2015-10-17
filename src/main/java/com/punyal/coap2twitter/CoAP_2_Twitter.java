/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.punyal.coap2twitter;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.CoapExchange;


import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import twitter4j.conf.ConfigurationBuilder;

/**
 *
 * @author Pablo Puñal Pereira <pablo.punal@ltu.se>
 */
public class CoAP_2_Twitter {
    
    public static void main(String[] args) {

            CoapServer server = new CoapServer(5680);
            server.add(new TwitterResource());
            server.start();
    }

    private static class TwitterResource  extends CoapResource{

        public TwitterResource() {
            super("twitter");
            getAttributes().setTitle("Twitter Resource");
        }
        
        @Override
        public void handleGET(CoapExchange exchange) {
            exchange.respond("Twitter Resource");
        }
        
        @Override
        public void handlePUT(CoapExchange exchange) {
            System.out.println(exchange.getRequestText());
            try {
                ConfigurationBuilder cb = new ConfigurationBuilder();
                cb.setDebugEnabled(true)
                  .setOAuthConsumerKey("")
                  .setOAuthConsumerSecret("")
                  .setOAuthAccessToken("")
                  .setOAuthAccessTokenSecret("");
                TwitterFactory tf = new TwitterFactory(cb.build());
                Twitter twitter = tf.getInstance();
                try {
                    // get request token.
                    // this will throw IllegalStateException if access token is already available
                    RequestToken requestToken = twitter.getOAuthRequestToken();
                    System.out.println("Got request token.");
                    System.out.println("Request token: " + requestToken.getToken());
                    System.out.println("Request token secret: " + requestToken.getTokenSecret());
                    AccessToken accessToken = null;

                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    while (null == accessToken) {
                        System.out.println("Open the following URL and grant access to your account:");
                        System.out.println(requestToken.getAuthorizationURL());
                        System.out.print("Enter the PIN(if available) and hit enter after you granted access.[PIN]:");
                        String pin = br.readLine();
                        try {
                            if (pin.length() > 0) {
                                accessToken = twitter.getOAuthAccessToken(requestToken, pin);
                            } else {
                                accessToken = twitter.getOAuthAccessToken(requestToken);
                            }
                        } catch (TwitterException te) {
                            if (401 == te.getStatusCode()) {
                                System.out.println("Unable to get the access token.");
                            } else {
                                te.printStackTrace();
                            }
                        }
                    }
                    System.out.println("Got access token.");
                    System.out.println("Access token: " + accessToken.getToken());
                    System.out.println("Access token secret: " + accessToken.getTokenSecret());
                } catch (IllegalStateException ie) {
                    // access token is already available, or consumer key/secret is not set.
                    if (!twitter.getAuthorization().isEnabled()) {
                        System.out.println("OAuth consumer key/secret is not set.");
                        System.exit(-1);
                    }
                }
                Status status = twitter.updateStatus(exchange.getRequestText());
                System.out.println("Successfully updated the status to [" + status.getText() + "].");
                exchange.respond("Successfully updated the status to [" + status.getText() + "].");
                //System.exit(0);
            } catch (TwitterException te) {
                te.printStackTrace();
                System.out.println("Failed to get timeline: " + te.getMessage());
                exchange.respond("Failed to get timeline: " + te.getMessage());
                //System.exit(-1);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.out.println("Failed to read the system input.");
                exchange.respond("Failed to read the system input.");
                //System.exit(-1);
            }
            
            
        }
        
    }
}
