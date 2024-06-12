/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.metrics;

import com.facebook.ads.sdk.APIContext;
import com.facebook.ads.sdk.APIException;
import com.facebook.ads.sdk.serverside.*;
import com.meta.cp4m.llm.HuggingFaceLlamaPlugin;
import com.meta.cp4m.llm.OpenAIPlugin;
import com.meta.cp4m.message.FBMessageHandler;
import com.meta.cp4m.message.MessageHandler;
import com.meta.cp4m.message.WAMessageHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class CAPIMetricsCollector implements MetricsCollector {
    record EventRecord<T extends MessageHandler<?>, E extends OpenAIPlugin<?>>(String name, UUID instanceId, Class<T> messengerType, Class<E> pluginType, String appId, String businessContactIdentifier) {}
    private final ConcurrentHashMap<EventRecord, Long> messageCounts = new ConcurrentHashMap<>();
    private static final CAPIMetricsCollector HANDLER = new CAPIMetricsCollector();
    public CAPIMetricsCollector() {
        ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(1);
        threadPool.scheduleAtFixedRate(this::sendAllMetrics, 1, 5, TimeUnit.MINUTES);
    }
    private void sendSingleMetric() {

        String ACCESS_TOKEN = "EAAKxnuilYZAQBO2dbiWA3r8z2FMzdmU2ZAfS3idlE1mzRXbZBJWDnZBmJNihs1CYz8GRZBl83jaErtsckFr8M88tWHUafREOEvFfDsq9vvSByks57y3sv5DZCa5IUqCSSjDwR4OJS2QkBogSvZAoCyO0KDJn69RzZBrQZCPeCk87PvfRf3yJP7gYQdL6H1dSD5BjSZCMN2EmgVM5aELrhGh3EZD";
        String PIXEL_ID = "1452308421990109";
        APIContext context = new APIContext(ACCESS_TOKEN);

        List<Event> events = new ArrayList<>();

        UserData userData_0 = new UserData()
                .clientIpAddress("207.96.79.161");


            CustomData customData_0 = new CustomData();
            HashMap<String, String> customProperties_0 = new HashMap<String, String>();
            customProperties_0.put("plugin", "openai");
            customProperties_0.put("messenger", "whatsapp");
            customProperties_0.put("business_contact_ID", "3333333333");
            customData_0.customProperties(customProperties_0);

            Event event_0 = new Event()
                    .eventName("WAReceivedJSON")
                    .eventTime(1717122946L)
                    .userData(userData_0)
                    .customData(customData_0)
                    .actionSource(ActionSource.valueOf("website"));

            events.add(event_0);
            EventRequest eventRequest = new EventRequest(PIXEL_ID, context)
                    .data(events);

        try {
            EventResponse response = eventRequest.execute();
            System.out.printf("Standard API response : %s ", response);
        } catch (APIException e) {
            e.printStackTrace();
        }
    }

    private void sendAllMetrics() {
        String ACCESS_TOKEN = "EAAKxnuilYZAQBO2dbiWA3r8z2FMzdmU2ZAfS3idlE1mzRXbZBJWDnZBmJNihs1CYz8GRZBl83jaErtsckFr8M88tWHUafREOEvFfDsq9vvSByks57y3sv5DZCa5IUqCSSjDwR4OJS2QkBogSvZAoCyO0KDJn69RzZBrQZCPeCk87PvfRf3yJP7gYQdL6H1dSD5BjSZCMN2EmgVM5aELrhGh3EZD";
        String PIXEL_ID = "1452308421990109";

        APIContext context = new APIContext(ACCESS_TOKEN);

        List<Event> events = new ArrayList<>();

        messageCounts.forEach((event, count) -> {
            CustomData customData_0 = new CustomData();
            HashMap<String, String> customProperties_0 = new HashMap<String, String>();


            UserData userData_0 = new UserData()
                    .clientIpAddress("207.96.79.161")
                    .clientUserAgent("cp4m")
                    .countryCodes(Arrays.asList("79adb2a2fce5c6ba215fe5f27f532d4e7edbac4b6a5e09e1ef3a08084a904621"));
            String eventMessengerType = "";
            if (event.messengerType.equals(FBMessageHandler.class)) {
                eventMessengerType = "FBMessenger";
            } else if (event.messengerType.equals(WAMessageHandler.class)) {
                eventMessengerType = "WhatsApp";
            }

            String eventPluginType = "";
            if (event.pluginType.equals(OpenAIPlugin.class)) {
                eventPluginType = "FBMessenger";
            } else if (event.pluginType.equals(HuggingFaceLlamaPlugin.class)) {
                eventPluginType = "WhatsApp";
            }

            long currentTimeInSeconds = System.currentTimeMillis() / 1000;

            customProperties_0.put("messenger_type", eventMessengerType);
            customProperties_0.put("plugin_type", eventPluginType);
            customProperties_0.put("business_contact_ID", event.businessContactIdentifier);
            customProperties_0.put("instance_id", String.valueOf(event.instanceId));
            customProperties_0.put("app_id", event.appId);

            customData_0.customProperties(customProperties_0);

            Event event_0 = new Event()
                    .eventName(event.name)
                    .eventTime(currentTimeInSeconds)
                    .userData(userData_0)
                    .customData(customData_0)
                    .actionSource(ActionSource.valueOf("website"));

            events.add(event_0);
        });

        EventRequest eventRequest = new EventRequest(PIXEL_ID, context)
                .data(events);

        try {
            EventResponse response = eventRequest.execute();
            System.out.printf("Standard API response : %s ", response);
        } catch (APIException e) {
            e.printStackTrace();
        }

    }
    @Override
    public <T extends MessageHandler<?>, E extends OpenAIPlugin<?>> void logMessageReceived(String name, UUID instanceId, Class<T> messengerType, Class<E> pluginType, String appId, String businessContactIdentifier) {
            messageCounts.merge(new EventRecord(name, instanceId, messengerType, pluginType, appId, businessContactIdentifier), 1L, Long::sum);
    }
    @Override
    public <T extends MessageHandler<?>, E extends OpenAIPlugin<?>> void logMessageSent(String name, UUID instanceId, Class<T> messengerType, Class<E> pluginType, String appId, String businessContactIdentifier) {
        messageCounts.merge(new EventRecord(name, instanceId, messengerType, pluginType, appId, businessContactIdentifier), 1L, Long::sum);
    }
    public static void main(String[] args) {
        CAPIMetricsCollector collector = HANDLER;
        collector.logMessageReceived("ReceivedTest", UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), FBMessageHandler.class, OpenAIPlugin.class, "1234", "1234");
        collector.logMessageSent("SentTest", UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), FBMessageHandler.class, OpenAIPlugin.class, "1234", "1234");
        collector.sendAllMetrics();
    }
}