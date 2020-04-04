package no.nav.helse.spedisjon

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*

internal class NyeSøknader(
    rapidsConnection: RapidsConnection,
    private val meldingDao: MeldingDao,
    private val problemsCollector: ProblemsCollector,
    private val aktørregisteretClient: AktørregisteretClient
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.forbid("@event_name") }
            validate { it.requireKey("aktorId", "arbeidsgiver.orgnummer", "soknadsperioder") }
            validate { it.require("opprettet", JsonNode::asLocalDateTime) }
            validate { it.requireValue("status", "NY") }
            validate { it.requireKey("id", "sykmeldingId", "fom", "tom") }
            validate { it.interestedIn("fnr") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        packet.putIfAbsent("fnr") { aktørregisteretClient.hentFødselsnummer(packet["aktorId"].asText())}
        val nySøknad = Melding.NySøknad(packet)
        if (!meldingDao.leggInn(nySøknad)) return
        context.send(nySøknad.fødselsnummer(), nySøknad.json())
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        problemsCollector.add("Ny søknad", problems)
    }
}
