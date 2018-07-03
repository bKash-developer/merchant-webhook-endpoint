package com.bkash.rnd.controller;

import com.bkash.rnd.models.Message;
import com.bkash.rnd.utility.WebhookUtility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by alam.ashraful on 6/3/2018.
 */
@RestController
@RequestMapping("/webhook")
public class WebhookController {
    private static final Logger LOG = LoggerFactory.getLogger(WebhookController.class);

    @CrossOrigin(origins = "*")
    @PostMapping("/endpoint")
    public ResponseEntity<?> consumeMessage(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {

        String messagetype = httpServletRequest.getHeader("x-amz-sns-message-type");
        LOG.info("BkashPgwWebhookController:: consumeMessage:: Header:: messagetype :" + messagetype);

        LOG.info("BkashPgwWebhookController:: consumeMessage:: body:: " + httpServletRequest.getInputStream());
        Scanner scan = new Scanner(httpServletRequest.getInputStream());
        StringBuilder builder = new StringBuilder();
        while (scan.hasNextLine()) {
            builder.append(scan.nextLine());
        }
        LOG.info("BkashPgwWebhookController:: consumeMessage:: body:: " + builder.toString());

        Gson gson = new GsonBuilder().create();
        Message msg = gson.fromJson(builder.toString(), Message.class);
        LOG.info("BkashPgwWebhookController:: consumeMessage:: msg:: " + msg.getSignatureVersion());

        if (msg.getSignatureVersion().equals("1")) {
            LOG.info("BkashPgwWebhookController:: Signature verification started....");
            if (WebhookUtility.isMessageSignatureValid(msg)) {
                LOG.info("BkashPgwWebhookController:: Signature verification succeeded");
            } else {
                LOG.info("BkashPgwWebhookController:: Signature verification failed");
                throw new SecurityException("Signature verification failed.");
            }
        } else {
            LOG.info("BkashPgwWebhookController:: Unexpected signature version. Unable to verify signature.");
        }

        if (messagetype.equals("Notification")) {
            LOG.info("BkashPgwWebhookController:: Notification.");
            String logMsgAndSubject = "";
            if (msg.getSubject() != null) {
                logMsgAndSubject += " Subject: " + msg.getSubject();
            }
            logMsgAndSubject += "Message: " + msg.getMessage();
            LOG.info("BkashPgwWebhookController:: "+logMsgAndSubject);
        } else if (messagetype.equals("SubscriptionConfirmation")) {
            LOG.info("BkashPgwWebhookController:: SubscriptionConfirmation.");
            Scanner sc = new Scanner(new URL(msg.getSubscribeURL()).openStream());
            StringBuilder sb = new StringBuilder();
            while (sc.hasNextLine()) {
                sb.append(sc.nextLine());
            }
            LOG.info("BkashPgwWebhookController:: Subscription confirmation (" + msg.getSubscribeURL() + ") Return value: " + sb.toString());
        } else if (messagetype.equals("UnsubscribeConfirmation")) {
            LOG.info("BkashPgwWebhookController:: UnsubscribeConfirmation.");
            LOG.info("BkashPgwWebhookController:: Unsubscribe confirmation: " + msg.getMessage());
        } else {
            LOG.info("BkashPgwWebhookController:: Unknown message type.");
        }
        LOG.info("BkashPgwWebhookController:: Done processing message: " + msg.getMessageId());

        return new ResponseEntity(HttpStatus.OK);
    }
}
