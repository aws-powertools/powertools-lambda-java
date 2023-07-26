package software.amazon.lambda.powertools.largemessages.internal;

import com.amazonaws.services.lambda.runtime.events.SQSEvent.MessageAttribute;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Md5Utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class LargeSQSMessageProcessor extends LargeMessageProcessor<SQSMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(LargeSQSMessageProcessor.class);
    private static final String LEGACY_RESERVED_ATTRIBUTE_NAME = "SQSLargePayloadSize";
    private static final int INTEGER_SIZE_IN_BYTES = 4;
    private static final byte STRING_TYPE_FIELD_INDEX = 1;
    private static final byte BINARY_TYPE_FIELD_INDEX = 2;
    private static final byte STRING_LIST_TYPE_FIELD_INDEX = 3;
    private static final byte BINARY_LIST_TYPE_FIELD_INDEX = 4;

    @Override
    protected String getMessageId(SQSMessage message) {
        return message.getMessageId();
    }

    @Override
    protected String getMessageContent(SQSMessage message) {
        return message.getBody();
    }

    @Override
    protected void updateMessageContent(SQSMessage message, String messageContent) {
        message.setBody(messageContent);
        // we update the MD5 digest so it doesn't look tempered
        message.setMd5OfBody(calculateMessageBodyMd5(messageContent).orElse(message.getMd5OfBody()));
    }

    @Override
    protected boolean isLargeMessage(SQSMessage message) {
        Map<String, MessageAttribute> msgAttributes = message.getMessageAttributes();
        return msgAttributes != null && (msgAttributes.containsKey(RESERVED_ATTRIBUTE_NAME) || msgAttributes.containsKey(LEGACY_RESERVED_ATTRIBUTE_NAME));
    }

    @Override
    protected void removeLargeMessageAttributes(SQSMessage message) {
        // message.getMessageAttributes() does not support remove operation, copy to new map
        Map<String, MessageAttribute> newAttributes = new HashMap<>(message.getMessageAttributes());
        newAttributes.remove(RESERVED_ATTRIBUTE_NAME);
        newAttributes.remove(LEGACY_RESERVED_ATTRIBUTE_NAME);
        message.setMessageAttributes(newAttributes);
        // we update the MD5 digest so it doesn't look tempered
        message.setMd5OfMessageAttributes(calculateMessageAttributesMd5(newAttributes).orElse(message.getMd5OfMessageAttributes()));
    }

    /**
     * Compute the MD5 of the message body.<br/>
     * Inspired from {@code software.amazon.awssdk.services.sqs.internal.MessageMD5ChecksumInterceptor}.<br/>
     * package protected for testing purpose.
     *
     * @param messageBody body of the SQS Message
     * @return the MD5 digest of the SQS Message body (or empty in case of error)
     */
    static Optional<String> calculateMessageBodyMd5(String messageBody) {
        byte[] expectedMd5;
        try {
            expectedMd5 = Md5Utils.computeMD5Hash(messageBody.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOG.warn("Unable to calculate the MD5 hash of the message body. ", e);
            return Optional.empty();
        }
        return Optional.of(BinaryUtils.toHex(expectedMd5));
    }

    /**
     * Compute the MD5 of the message attributes.<br/>
     * Inspired from {@code software.amazon.awssdk.services.sqs.internal.MessageMD5ChecksumInterceptor}.<br/>
     * package protected for testing purpose.
     *
     * @param messageAttributes attributes of the SQS Message
     * @return the MD5 digest of the SQS Message attributes (or empty in case of error)
     */
    @SuppressWarnings("squid:S4790") // MD5 algorithm is used by SQS, we must use MD5
    static Optional<String> calculateMessageAttributesMd5(final Map<String, MessageAttribute> messageAttributes) {
        List<String> sortedAttributeNames = new ArrayList<>(messageAttributes.keySet());
        Collections.sort(sortedAttributeNames);

        MessageDigest md5Digest;
        try {
            md5Digest = MessageDigest.getInstance("MD5");

            for (String attrName : sortedAttributeNames) {
                MessageAttribute attrValue = messageAttributes.get(attrName);

                // Encoded Name
                updateLengthAndBytes(md5Digest, attrName);

                // Encoded Type
                updateLengthAndBytes(md5Digest, attrValue.getDataType());

                // Encoded Value
                if (attrValue.getStringValue() != null) {
                    md5Digest.update(STRING_TYPE_FIELD_INDEX);
                    updateLengthAndBytes(md5Digest, attrValue.getStringValue());
                } else if (attrValue.getBinaryValue() != null) {
                    md5Digest.update(BINARY_TYPE_FIELD_INDEX);
                    updateLengthAndBytes(md5Digest, attrValue.getBinaryValue());
                } else if (attrValue.getStringListValues() != null &&
                        attrValue.getStringListValues().size() > 0) {
                    md5Digest.update(STRING_LIST_TYPE_FIELD_INDEX);
                    for (String strListMember : attrValue.getStringListValues()) {
                        updateLengthAndBytes(md5Digest, strListMember);
                    }
                } else if (attrValue.getBinaryListValues() != null &&
                        attrValue.getBinaryListValues().size() > 0) {
                    md5Digest.update(BINARY_LIST_TYPE_FIELD_INDEX);
                    for (ByteBuffer byteListMember : attrValue.getBinaryListValues()) {
                        updateLengthAndBytes(md5Digest, byteListMember);
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Unable to calculate the MD5 hash of the message attributes. ", e);
            return Optional.empty();
        }

        return Optional.of(BinaryUtils.toHex(md5Digest.digest()));
    }

    /**
     * Update the digest using a sequence of bytes that consists of the length (in 4 bytes) of the
     * input String and the actual utf8-encoded byte values.
     */
    private static void updateLengthAndBytes(MessageDigest digest, String str) {
        byte[] utf8Encoded = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer lengthBytes = ByteBuffer.allocate(INTEGER_SIZE_IN_BYTES).putInt(utf8Encoded.length);
        digest.update(lengthBytes.array());
        digest.update(utf8Encoded);
    }

    /**
     * Update the digest using a sequence of bytes that consists of the length (in 4 bytes) of the
     * input ByteBuffer and all the bytes it contains.
     */
    private static void updateLengthAndBytes(MessageDigest digest, ByteBuffer binaryValue) {
        ByteBuffer readOnlyBuffer = binaryValue.asReadOnlyBuffer();
        int size = readOnlyBuffer.remaining();
        ByteBuffer lengthBytes = ByteBuffer.allocate(INTEGER_SIZE_IN_BYTES).putInt(size);
        digest.update(lengthBytes.array());
        digest.update(readOnlyBuffer);
    }
}
