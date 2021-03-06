package pl.futurecollars.invoicing.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.json.JacksonTester
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.PostgreSQLContainer
import pl.futurecollars.invoicing.dto.InvoiceDto
import pl.futurecollars.invoicing.dto.mappers.InvoiceMapper
import pl.futurecollars.invoicing.fixtures.InvoiceFixture
import pl.futurecollars.invoicing.model.Invoice
import pl.futurecollars.invoicing.model.InvoiceEntry
import pl.futurecollars.invoicing.repository.InvoiceRepository
import spock.lang.Specification
import spock.lang.Subject
import java.util.stream.Collectors
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@AutoConfigureJsonTesters
@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("testcontainer")
@WithMockUser(authorities = "USER")
class InvoiceControllerTest extends Specification {

    @Subject.Container
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test")

    static {
        postgreSQLContainer.start()
        System.setProperty("DB_PORT", String.valueOf(postgreSQLContainer.getFirstMappedPort()))
    }

    @Autowired
    MockMvc mockMvc

    @Autowired
    InvoiceRepository invoiceRepository

    @Autowired
    JacksonTester<InvoiceDto> jsonService

    @Autowired
    JacksonTester<List<InvoiceDto>> jsonListService

    @Autowired
    InvoiceMapper invoiceMapper

    InvoiceDto invoiceDto = InvoiceFixture.getInvoiceDto(1)

    def setup() {
        deleteAllInvoices()
    }

    def "should return empty list"() {
        when:
        def response = mockMvc
                .perform(get("/api/invoices"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()

        then:
        response == "[]"
    }

    def "should save invoice to database"() {
        given:
        String jsonString = jsonService.write(invoiceDto).getJson()

        when:
        def response = mockMvc
                .perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()
        InvoiceDto responseDto = jsonService.parseObject(response)
        invoiceDto.setId(responseDto.getId())
        invoiceDto.getSeller().setId(responseDto.getSeller().getId())
        invoiceDto.getBuyer().setId(responseDto.getBuyer().getId())
        invoiceDto.getInvoiceEntries().get(0).setId(responseDto.getInvoiceEntries().get(0).getId())


        then:
        responseDto == invoiceDto
    }

    def "should return 400 Bad Request if number is already in use"() {
        given:
        InvoiceDto returnedInvoiceDto = addInvoices(1).get(0)
        InvoiceDto invoiceToSave = InvoiceFixture.getInvoiceDto(1)
        invoiceToSave.setNumber(returnedInvoiceDto.getNumber())
        String jsonString = jsonService.write(invoiceToSave).getJson()

        expect:
        def response = mockMvc
                .perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString))
                .andExpect(status().isBadRequest())
    }

    def "should return list of all invoices"() {
        given:
        int numberOfInvoicesAdded = 10
        def expectedInvoices = addInvoices(numberOfInvoicesAdded)
        Comparator<InvoiceDto> comparator = (o1, o2) -> {o1.getId().compareTo(o2.getId())}

        when:
        def response = mockMvc
                .perform(get("/api/invoices"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()


        then:
        def invoices = jsonListService.parseObject(response)
        invoices.size() == numberOfInvoicesAdded
        invoices.sort(comparator) == expectedInvoices.sort(comparator)
    }

    def "should return invoice by id"() {
        given:
        def invoices = addInvoices(3)
        InvoiceDto invoiceDto = invoices[0]
        UUID id = invoiceDto.getId()

        when:
        def response = mockMvc
                .perform(get("/api/invoices/" + id.toString()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()

        then:
        jsonService.parseObject(response) == invoiceDto
    }

    def "should return 404 NotFound status when asking for nonexistent invoice by id"() {
        given:
        UUID invalidId = UUID.randomUUID()

        expect:
        mockMvc
                .perform(get("/api/invoices/" + invalidId.toString()))
                .andExpect(status().isNotFound())
    }

    def "should update invoice"() {
        given:
        def invoices = addInvoices(3)
        InvoiceDto invoiceToUpdate = invoices[1]
        InvoiceDto updatedInvoice = InvoiceFixture.getInvoiceDto(1)
        updatedInvoice.setId(invoiceToUpdate.getId())
        String jsonString = jsonService.write(updatedInvoice).getJson()

        when:
        def response = mockMvc
                .perform(put("/api/invoices/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()

        then:
        InvoiceDto responseDto = getInvoiceById(invoiceToUpdate.getId())
        responseDto.getId() == updatedInvoice.getId()
        responseDto.getNumber() == updatedInvoice.getNumber()
        responseDto.getSeller().getTaxIdentificationNumber() == updatedInvoice.getSeller().getTaxIdentificationNumber()
        responseDto.getBuyer().getTaxIdentificationNumber() == updatedInvoice.getBuyer().getTaxIdentificationNumber()
    }

    def "should return 404 NotFound when updating nonexistent invoice"() {
        given:
        String jsonString = jsonService.write(invoiceDto).getJson()

        expect:
        mockMvc
                .perform(put("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString))
                .andExpect(status().isNotFound())
    }

    def "should return invoices created before given date"() {
        given:
        addInvoices(3)

        when:
        def response = mockMvc
                .perform(get("/api/invoices")
                        .queryParam("before", "2020-10-10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()

        then:
        response == "[]"
    }

    def "should filter and return invoices created after given date"() {
        given:
        def numberOfInvoices = 10
        def invoices = addInvoices(numberOfInvoices)
        Comparator<InvoiceDto> comparator = (o1, o2) -> {o1.getId().compareTo(o2.getId())}

        when:
        def response = mockMvc
                .perform(get("/api/invoices")
                        .queryParam("after", "2020-10-10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()

        then:
        def returnedInvoices = jsonListService.parseObject(response)
        returnedInvoices.size() == numberOfInvoices
        returnedInvoices.sort(comparator) == invoices.sort(comparator)
    }

    def "should filter invoices by sellerTaxId"() {
        given:
        def invoices = addInvoices(10)
        def invoice = invoices[0]
        String sellerTaxId = invoice.getSeller().getTaxIdentificationNumber()
        Comparator<InvoiceEntry> comparator = (o1, o2) -> {o1.getId().compareTo(o2.getId())}

        when:
        def response = mockMvc
                .perform(get("/api/invoices")
                        .queryParam("sellerTaxId", sellerTaxId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()

        then:
        def filteredInvoices = jsonListService.parseObject(response)
        filteredInvoices.size() == 1
        filteredInvoices[0] == invoice
    }

    def "should filter invoices by buyerTaxId"() {
        given:
        def invoices = addInvoices(10)
        def invoice = invoices[0]
        String buyerTaxId = invoice.getBuyer().getTaxIdentificationNumber()
        Comparator<InvoiceEntry> comparator = (o1, o2) -> {o1.getId().compareTo(o2.getId())}

        when:
        def response = mockMvc
                .perform(get("/api/invoices")
                        .queryParam("buyerTaxId", buyerTaxId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()

        then:
        def filteredInvoices = jsonListService.parseObject(response)
        filteredInvoices.size() == 1
        filteredInvoices[0] == invoice
    }

    def "should delete invoice"() {
        given:
        def numberOfInvoices = 3
        def invoices = addInvoices(numberOfInvoices)
        InvoiceDto toDelete = invoices[0]
        UUID id = toDelete.getId()

        when:
        mockMvc
                .perform(delete("/api/invoices/" + id.toString()))
                .andExpect(status().isAccepted())

        then:
        getAllInvoices().size() == numberOfInvoices - 1
    }

    def "should return 404 NotFound when deleting nonexistent invoice"() {
        given:
        UUID id = invoiceDto.getId()

        expect:
        mockMvc
                .perform(delete("/api/invoices/" + id.toString()))
                .andExpect(status().isNotFound())
    }


    private List<InvoiceDto> addInvoices(int number) {
        List<InvoiceDto> invoiceList = new ArrayList<>()
        for(int i = 0; i < number; i++) {
           Invoice invoice = InvoiceFixture.getInvoice(1)
            Invoice returnedInvoice = invoiceRepository.save(invoice)
            invoiceList.add(invoiceMapper.toDto(returnedInvoice))
        }
        return invoiceList
    }

    private List<InvoiceDto> getAllInvoices() {
        return invoiceRepository.findAll()
                .stream()
                .map(i -> invoiceMapper.toDto(i))
                .collect(Collectors.toList())
    }

    private void deleteAllInvoices() {
        invoiceRepository.deleteAll()
    }

    private InvoiceDto getInvoiceById(UUID id) {
        return invoiceMapper.toDto(invoiceRepository.findById(id).get())
    }
}
