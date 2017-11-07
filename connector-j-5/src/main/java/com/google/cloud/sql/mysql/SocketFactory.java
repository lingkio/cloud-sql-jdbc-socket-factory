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

package com.google.cloud.sql.mysql;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.cloud.sql.core.SslSocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A MySQL {@link SocketFactory} that establishes a secure connection to a Cloud SQL instance using
 * ephemeral certificates.
 *
 * <p>The heavy lifting is done by the singleton {@link SslSocketFactory} class.
 */
public class SocketFactory implements com.mysql.jdbc.SocketFactory {
  private static final Logger logger = Logger.getLogger(SocketFactory.class.getName());

  private Socket socket;

  @Override
  public Socket connect(String hostname, int portNumber, Properties props) throws IOException {
    String[] instanceNameAndCredential = props.getProperty("cloudSqlInstanceAndCredential").split(":::" );
    String instanceName = instanceNameAndCredential[0];
    String credential = instanceNameAndCredential[1];

    checkArgument(
            instanceName != "",
            "cloudSqlInstanceAndCredential property not set. Please specify this property in the JDBC URL or "
                    + "the connection Properties with value in form \"project:region:instance\" along with a credential.");

    checkArgument(
            credential != "",
            "cloudSqlInstanceAndCredential property not set. Please specify this property in the JDBC URL or "
                    + "the connection Properties with value in form \"project:region:instance\" along with a credential.");


    logger.info(String.format("Connecting to Cloud SQL instance [%s].", instanceName));


    String temp = credential;
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

    String credential_json = temp.substring(0, begin+begin_length) + private_key + temp.substring(end, temp.length());

    this.socket = SslSocketFactory.getInstance(credential_json).create(instanceName);
    return this.socket;
  }

  @Override
  public Socket beforeHandshake() {
    return socket;
  }

  @Override
  public Socket afterHandshake() {
    return socket;
  }
}
