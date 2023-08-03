import com.amazonaws.lambda.thirdparty.com.fasterxml.jackson.databind.ObjectMapper;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.serialization.PojoSerializer;
import com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers;
import com.amazonaws.services.lambda.runtime.serialization.factories.JacksonFactory;
import software.amazon.lambda.powertools.e2e.Function;

public class TestSerialization {

    static String sqsMsg = "{\"Records\":[{\"messageId\":\"0c957063-07a7-4495-ab2a-d4678ce8c641\",\"receiptHandle\":\"AQEBJUG48GuQg8eTMDe8roYS4HKyxnramDvOYG+HqQrZkH/fz1ZQRoXzMvYiVsod/eR5wYviLr2RL+eX1kcqJfLhugRErFonuuF7mNj6RnOcBUHRxYrlCc5yP84WRGkRXCFhZ0esYNOgL4SABkVkJxOm72wmkz2PD5stzxpBq+SzrLEIroyx3TJ+2Tmp1m5lDHAZnrj9b3LcRBBF2JJ1FPibQ1WcXnmzWZxtefAbUppWxPM7M1yeTgJNuPMd9/r8u3RDl5AXCfao/FTMPAWrvi3Ih7FJUWD/jwCXC4sa9X/xOc2pyQBqsrkQWTANDh20XHMYYp5wcvj+ceom+BZ1NfmotLLtutoP7DMR2d5M3oAx5+me251jtBulq4LGHRgzdtSJcxJlFGXunWTZbG78u61BxQ==\",\"body\":\"{\\\"id\\\":1,\\\"name\\\":\\\"product1\\\",\\\"price\\\":1.23}\",\"attributes\":{\"ApproximateReceiveCount\":\"1\",\"SentTimestamp\":\"1691046046134\",\"SenderId\":\"AROAT2IWOUCAKCSJQ5M5X:gerrings-Isengard\",\"ApproximateFirstReceiveTimestamp\":\"1691046046146\"},\"messageAttributes\":{},\"md5OfBody\":\"6070c6b8b6961c80279de16f37838cc6\",\"eventSource\":\"aws:sqs\",\"eventSourceARN\":\"arn:aws:sqs:eu-west-1:262576971904:batchqueuefdedf4\",\"awsRegion\":\"eu-west-1\"}]}";
    static String kinesisMsg = "{\"Records\":[{\"kinesis\":{\"kinesisSchemaVersion\":\"1.0\",\"partitionKey\":\"1\",\"sequenceNumber\":\"49643250745023848610933107459883370427365077215172624434\",\"data\":\"eyJpZCI6MSwibmFtZSI6InByb2R1Y3QxIiwicHJpY2UiOjEuMjN9\",\"approximateArrivalTimestamp\":1.69106871717E9},\"eventSource\":\"aws:kinesis\",\"eventVersion\":\"1.0\",\"eventID\":\"shardId-000000000003:49643250745023848610933107459883370427365077215172624434\",\"eventName\":\"aws:kinesis:record\",\"invokeIdentityArn\":\"arn:aws:iam::262576971904:role/BatchE2ET-2b1e1d07d19a-BatchE2ET2b1e1d07d19afuncti-W4MDBKGVWKQA\",\"awsRegion\":\"eu-west-1\",\"eventSourceARN\":\"arn:aws:kinesis:eu-west-1:262576971904:stream/batchstream261626\"},{\"kinesis\":{\"kinesisSchemaVersion\":\"1.0\",\"partitionKey\":\"1\",\"sequenceNumber\":\"49643250745023848610933107459884579353184691844347330610\",\"data\":\"eyJpZCI6MiwibmFtZSI6InByb2R1Y3QyIiwicHJpY2UiOjQuNTZ9\",\"approximateArrivalTimestamp\":1.691068717172E9},\"eventSource\":\"aws:kinesis\",\"eventVersion\":\"1.0\",\"eventID\":\"shardId-000000000003:49643250745023848610933107459884579353184691844347330610\",\"eventName\":\"aws:kinesis:record\",\"invokeIdentityArn\":\"arn:aws:iam::262576971904:role/BatchE2ET-2b1e1d07d19a-BatchE2ET2b1e1d07d19afuncti-W4MDBKGVWKQA\",\"awsRegion\":\"eu-west-1\",\"eventSourceARN\":\"arn:aws:kinesis:eu-west-1:262576971904:stream/batchstream261626\"},{\"kinesis\":{\"kinesisSchemaVersion\":\"1.0\",\"partitionKey\":\"1\",\"sequenceNumber\":\"49643250745023848610933107459885788279004306473522036786\",\"data\":\"eyJpZCI6MywibmFtZSI6InByb2R1Y3QzIiwicHJpY2UiOjYuNzh9\",\"approximateArrivalTimestamp\":1.691068717172E9},\"eventSource\":\"aws:kinesis\",\"eventVersion\":\"1.0\",\"eventID\":\"shardId-000000000003:49643250745023848610933107459885788279004306473522036786\",\"eventName\":\"aws:kinesis:record\",\"invokeIdentityArn\":\"arn:aws:iam::262576971904:role/BatchE2ET-2b1e1d07d19a-BatchE2ET2b1e1d07d19afuncti-W4MDBKGVWKQA\",\"awsRegion\":\"eu-west-1\",\"eventSourceARN\":\"arn:aws:kinesis:eu-west-1:262576971904:stream/batchstream261626\"}]}";
    public static void main(String [] args) throws Exception {

        Function f = new Function();
        f.createResult(kinesisMsg, null);

        ObjectMapper mapper = JacksonFactory.getInstance().getMapper();

        PojoSerializer<SQSEvent> serializer = LambdaEventSerializers.serializerFor(SQSEvent.class, TestSerialization.class.getClassLoader());
        PojoSerializer<KinesisEvent> kinesisSerializer = LambdaEventSerializers.serializerFor(KinesisEvent.class, TestSerialization.class.getClassLoader());

        SQSEvent sqsAsSqs = serializer.fromJson(sqsMsg);
        SQSEvent kinesisAsSqs = serializer.fromJson(kinesisMsg);


        System.out.println(kinesisAsSqs);
    }
}
