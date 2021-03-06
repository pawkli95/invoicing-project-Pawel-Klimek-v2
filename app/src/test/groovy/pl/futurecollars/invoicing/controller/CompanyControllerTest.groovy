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
import pl.futurecollars.invoicing.dto.CompanyDto
import pl.futurecollars.invoicing.dto.mappers.CompanyMapper
import pl.futurecollars.invoicing.fixtures.CompanyFixture
import pl.futurecollars.invoicing.model.Company
import pl.futurecollars.invoicing.repository.CompanyRepository
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
class CompanyControllerTest extends Specification {

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
    CompanyRepository companyRepository

    CompanyDto companyDto = CompanyFixture.getCompanyDto()

    @Autowired
    JacksonTester<CompanyDto> jsonService

    @Autowired
    JacksonTester<List<CompanyDto>> jsonListService

    @Autowired
    CompanyMapper companyMapper

    def setup() {
        clearDatabase()
    }

    def "should save company to database"() {
        given:
        String jsonString = jsonService.write(companyDto).getJson()

        when:
        def response = mockMvc
                .perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()

        then:
        def company = jsonService.parseObject(response)
        company.getTaxIdentificationNumber() == companyDto.getTaxIdentificationNumber()
        company.getName() == companyDto.getName()
    }

    def "should return 400 Bad Request if tax id is already in use"() {
        given:
        CompanyDto returnedCompany = addCompany()
        CompanyDto companyToSave = CompanyFixture.getCompanyDto()
        companyToSave.setTaxIdentificationNumber(returnedCompany.getTaxIdentificationNumber())
        String jsonString = jsonService.write(companyToSave).getJson()

        expect:
        def response = mockMvc
                .perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString))
                .andExpect(status().isBadRequest())
    }

    def "should return company by id"() {
        given:
        def returnedCompaniesDto = addCompanies(5)
        CompanyDto returnedCompanyDto = returnedCompaniesDto.get(0)
        UUID id = returnedCompanyDto.getId()

        when:
        def response = mockMvc
                .perform(get("/api/companies/" + id.toString()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()

        then:
        jsonService.parseObject(response) == returnedCompanyDto
    }

    def "should return 404 NotFound status when getting company by nonexistent id"() {
        expect:
        mockMvc
                .perform(get("/api/companies/"+ UUID.randomUUID().toString()))
                .andExpect(status().isNotFound())
    }

    def "should return list of companies"() {
        given:
        def returnedList = addCompanies(10)

        when:
        def response = mockMvc
                .perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()

        then:
        def list = jsonListService.parseObject(response)
        list.size() == 10
        list == returnedList
    }

    def "should update company details"() {
        given:
        def returnedCompany = addCompany()
        def updatedCompany = CompanyFixture.getCompanyDto()
        updatedCompany.setId(returnedCompany.getId())
        String jsonString = jsonService.write(updatedCompany).getJson()

        when:
        def response = mockMvc
                .perform(put("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()

        then:
        jsonService.parseObject(response) == updatedCompany
    }

    def "should return 404 NotFound status when updating company which doesn't exist"() {
        given:
        String jsonString = jsonService.write(companyDto).getJson()

        expect:
        mockMvc
                .perform(put("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString))
                .andExpect(status().isNotFound())
    }

    def "should delete company"() {
        given:
        def list = addCompanies(10)
        def companyToDelete = list.get(0)
        UUID id = companyToDelete.getId()

        when:
        mockMvc
                .perform(delete("/api/companies/" + id.toString()))
                .andExpect(status().isAccepted())

        then:
        getAllCompanies().size() == 9
    }

    def "should return 404 NotFound status when deleting company with nonexistent id"() {
        expect:
        mockMvc
                .perform(delete("/api/companies/" + UUID.randomUUID().toString()))
                .andExpect(status().isNotFound())
    }

    def clearDatabase() {
        companyRepository.deleteAll()
    }

    def getAllCompanies() {
        return companyRepository.findAll()
                .stream()
                .map(companyMapper::toDto)
                .collect(Collectors.toList())
    }

    def List<CompanyDto> addCompanies(int number) {
        List<CompanyDto> list = new ArrayList<>()
        for(int i = 0; i < number; i++) {
           CompanyDto companyDto1 = addCompany()
            list.add(companyDto1)
        }
        return list
    }

    def CompanyDto addCompany() {
        Company company = CompanyFixture.getCompany()
        return companyMapper.toDto(companyRepository.save(company))
    }


}
