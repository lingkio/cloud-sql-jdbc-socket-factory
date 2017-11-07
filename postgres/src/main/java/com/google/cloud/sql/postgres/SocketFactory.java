/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.sql.postgres;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.cloud.sql.core.SslSocketFactory;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Postgres {@link SocketFactory} that establishes a secure connection to a Cloud SQL instance
 * using ephemeral certificates.
 *
 * <p>The heavy lifting is done by the singleton {@link SslSocketFactory} class.
 */
public class SocketFactory extends javax.net.SocketFactory {
  private static final Logger logger = Logger.getLogger(SocketFactory.class.getName());

  private  String instanceName = "";
  private  String credential_json = "";


  //public SocketFactory(String instanceName, String credential_json) {
  public SocketFactory(String instanceNameAndCredential_json) {
    checkArgument(
            instanceNameAndCredential_json.split(":::" )[0] != null,
            "socketFactoryArg property not set. Please specify this property in the JDBC "
                    + "URL or the connection Properties with the instance connection name in "
                    + "form \"project:region:instance\"" );

    this.instanceName = instanceNameAndCredential_json.split(":::" )[0];
    checkArgument(
            instanceNameAndCredential_json.split(":::" )[1] != null,
            "Please specify credential string in json format." );


    String temp = instanceNameAndCredential_json.split(":::" )[1];
    String str = temp;
    Pattern pattern = Pattern.compile("-----BEGIN PRIVATE KEY-----(.*)-----END PRIVATE KEY-----");
    Matcher matcher = pattern.matcher(str);
    String private_key = "";
    while (matcher.find()) {
      private_key = matcher.group(1);
    }
    private_key = private_key.replace(" ", "+");
    int begin = temp.indexOf("-----BEGIN PRIVATE KEY-----");
    int begin_length = "-----BEGIN PRIVATE KEY-----".length();

    int end = temp.indexOf("-----END PRIVATE KEY-----");

    this.credential_json = temp.substring(0, begin+begin_length) + private_key + temp.substring(end, temp.length());

  }


  @Override
  public Socket createSocket() throws IOException {
    logger.info(String.format("Connecting to Cloud SQL instance [%s].", instanceName));
    return SslSocketFactory.getInstance(this.credential_json).create(instanceName);
  }

  @Override
  public Socket createSocket(String host, int port) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
      throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Socket createSocket(InetAddress host, int port) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
      throws IOException {
    throw new UnsupportedOperationException();
  }
}
