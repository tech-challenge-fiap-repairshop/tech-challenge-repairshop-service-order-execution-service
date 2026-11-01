package com.cao.repairshop.core.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException

@Configuration
@ConditionalOnProperty(name = ["aws.sqs.auto-create-queues"], havingValue = "true", matchIfMissing = false)
class SqsQueueInitializer(
    private val sqsClient: SqsClient,
    @Value("\${sqs.queue.approve-notification:approve-notification-queue-dev}")
    private val approveNotificationQueue: String,
    @Value("\${sqs.queue.callback-approve:callback-approve-notification-queue-dev}")
    private val callbackApproveQueue: String,
    @Value("\${sqs.queue.os-ready-for-payment:os-ready-for-payment-queue-dev}")
    private val osReadyForPaymentQueue: String,
    @Value("\${sqs.queue.payment-success:payment-success-queue-dev}")
    private val paymentSuccessQueue: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun initializeQueues() {
        val queues = listOf(
            approveNotificationQueue,
            callbackApproveQueue,
            osReadyForPaymentQueue,
            paymentSuccessQueue
        )

        logger.info("⚡ Iniciando auto-criação de filas SQS...")
        for (queue in queues) {
            runCatching {
                try {
                    val url = sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queue).build()).queueUrl()
                    logger.info("✅ Fila SQS já existe: {} ({})", queue, url)
                } catch (e: QueueDoesNotExistException) {
                    val createResponse = sqsClient.createQueue(CreateQueueRequest.builder().queueName(queue).build())
                    logger.info("🎉 Fila SQS criada com sucesso: {} ({})", queue, createResponse.queueUrl())
                }
            }.onFailure { ex ->
                logger.warn("⚠️ Não foi possível verificar/criar fila SQS {}: {}", queue, ex.message)
            }
        }
    }
}

