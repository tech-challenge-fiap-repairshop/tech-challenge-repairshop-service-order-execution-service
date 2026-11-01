package com.cao.repairshop.bdd.steps

import com.cao.repairshop.core.exception.InvalidStateTransitionException
import com.cao.repairshop.execution.domain.ExecutionStatus
import com.cao.repairshop.execution.domain.entities.Insume
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import io.cucumber.java.pt.Dado
import io.cucumber.java.pt.E
import io.cucumber.java.pt.Entao
import io.cucumber.java.pt.Quando
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class ServiceOrderStepDefinitions {

    private lateinit var serviceOrder: ServiceOrder
    private var lastException: Exception? = null

    @Dado("que uma nova Ordem de Serviço é criada com status inicial {string}")
    fun queUmaNovaOrdemDeServicoECriada(statusInicial: String) {
        serviceOrder = ServiceOrder(
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = ServiceOrderStatus.valueOf(statusInicial),
            enterTime = LocalDateTime.now()
        )
        lastException = null
    }

    @Quando("o atendente adiciona o serviço {string} com valor de {double}")
    fun oAtendenteAdicionaOServico(nomeServico: String, valor: Double) {
        serviceOrder.addExecution(
            basicDescription = nomeServico,
            fullDescription = null,
            price = BigDecimal.valueOf(valor),
            estimatedTime = null,
            insumes = emptyList()
        )
    }

    @E("adiciona o insumo {string} com valor unitário de {double} e quantidade {int}")
    fun adicionaOInsumo(nomeInsumo: String, valorUnitario: Double, quantidade: Int) {
        val insume = Insume(
            id = UUID.randomUUID(),
            name = nomeInsumo,
            unityPrice = BigDecimal.valueOf(valorUnitario),
            quantity = quantidade
        )
        val exec = serviceOrder.executions.last()
        exec.addInsume(insume, quantidade)
        serviceOrder.recalculateTotalPrice()
    }

    @Entao("o valor total da Ordem de Serviço deve ser {double}")
    fun oValorTotalDaOrdemDeServicoDeveSer(valorEsperado: Double) {
        assertThat(serviceOrder.totalPrice).isEqualByComparingTo(BigDecimal.valueOf(valorEsperado))
    }

    @Quando("a Ordem de Serviço avança para o status {string}")
    fun aOrdemDeServicoAvancaParaOStatus(novoStatus: String) {
        serviceOrder.advanceStatus(ServiceOrderStatus.valueOf(novoStatus))
    }

    @E("o cliente aprova o orçamento")
    fun oClienteAprovaOOrcamento() {
        serviceOrder.approve()
    }

    @Entao("o status da Ordem de Serviço deve ser {string}")
    fun oStatusDaOrdemDeServicoDeveSer(statusEsperado: String) {
        assertThat(serviceOrder.status).isEqualTo(ServiceOrderStatus.valueOf(statusEsperado))
    }

    @E("o histórico deve conter o registro de transição para {string}")
    fun oHistoricoDeveConterORegistroDeTransicao(statusEsperado: String) {
        val status = ServiceOrderStatus.valueOf(statusEsperado)
        assertThat(serviceOrder.histories.any { it.status == status }).isTrue()
    }

    @Quando("o cliente recusa o orçamento")
    fun oClienteRecusaOOrcamento() {
        serviceOrder.refuse()
    }

    @E("a Ordem de Serviço não deve permitir avançar para {string}")
    fun aOrdemDeServicoNaoDevePermitirAvancarPara(statusInvalido: String) {
        val targetStatus = ServiceOrderStatus.valueOf(statusInvalido)
        assertThatThrownBy { serviceOrder.advanceStatus(targetStatus) }
            .isInstanceOf(InvalidStateTransitionException::class.java)
    }

    @Dado("que a Ordem de Serviço não possui nenhum serviço cadastrado")
    fun queAOrdemDeServicoNaoPossuiNenhumServico() {
        assertThat(serviceOrder.executions).isEmpty()
    }

    @Quando("o atendente tenta avançar a Ordem de Serviço para {string}")
    fun oAtendenteTentaAvancarAOrdemDeServicoPara(novoStatus: String) {
        try {
            serviceOrder.advanceStatus(ServiceOrderStatus.valueOf(novoStatus))
        } catch (ex: Exception) {
            lastException = ex
        }
    }

    @Entao("uma exceção de transição inválida deve ser lançada informando que não há serviços")
    fun umaExcecaoDeTransicaoInvalidaDeveSerLancada() {
        assertThat(lastException)
            .isNotNull
            .isInstanceOf(InvalidStateTransitionException::class.java)
            .hasMessageContaining("no services")
    }

    @E("o pagamento é confirmado e a Ordem de Serviço avança para {string}")
    fun oPagamentoEConfirmadoEOrdemAvancaPara(novoStatus: String) {
        serviceOrder.advanceStatus(ServiceOrderStatus.valueOf(novoStatus))
    }

    @E("o mecânico finaliza todas as tarefas do serviço")
    fun oMecanicoFinalizaTodasAsTarefasDoServico() {
        serviceOrder.executions.forEach { it.status = ExecutionStatus.FINALIZED }
    }

    @Quando("a Ordem de Serviço é verificada para conclusão")
    fun aOrdemDeServicoEVerificadaParaConclusao() {
        val completed = serviceOrder.checkCompletion()
        assertThat(completed).isTrue()
    }

    @E("a data de finalização deve ser registrada")
    fun aDataDeFinalizacaoDeveSerRegistrada() {
        assertThat(serviceOrder.endTime).isNotNull()
    }
}
