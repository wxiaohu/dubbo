/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting.transport.dispatcher.connection;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Dispatcher;

/**
 * connect disconnect ensure the order
 * 线程池调度策略：连接、取消连接以及接收到的消息都分发到线程池，但是不同的是，该类自己创建了一个跟连接相关的线程池，把连接操作和断开连接操分发到该线程池，而接收到的消息则分发到WrappedChannelHandler的线程池中
 */
public class ConnectionOrderedDispatcher implements Dispatcher {

    public static final String NAME = "connection";

    @Override
    public ChannelHandler dispatch(ChannelHandler handler, URL url) {
        return new ConnectionOrderedChannelHandler(handler, url);
    }

}
