package pl.futurecollars.invoicing.service.integrationTests

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

import pl.futurecollars.invoicing.repository.CompanyRepository
import pl.futurecollars.invoicing.repository.InvoiceRepository
import pl.futurecollars.invoicing.fixtures.CompanyFixture
import pl.futurecollars.invoicing.fixtures.InvoiceEntryFixture
import pl.futurecollars.invoicing.model.Company
import pl.futurecollars.invoicing.model.Invoice
import pl.futurecollars.invoicing.dto.TaxCalculation
import pl.futurecollars.invoicing.service.TaxCalculatorService
import spock.lang.Shared
import spock.lang.Specification
import java.time.LocalDate;

@SpringBootTest
@ActiveProfiles("test")
class TaxCalculatorServiceIntegrationTest extends Specification {

    @Autowired
    InvoiceRepository invoiceRepository

    @Autowired
    CompanyRepository companyRepository

    @Autowired
    TaxCalculatorService taxCalculatorService

    @Shared
    Company company1 = CompanyFixture.getCompany()

    def setup() {
        clearDatabase()
    }

    def "should calculate tax without personal car expenses"() {
        given: "database with invoices without personal car entries"
        addInvoicesWithoutPersonalCarEntries()

        when: "we ask taxCalculatorService to calculate taxes"
        TaxCalculation taxCalculation = taxCalculatorService.getTaxCalculation(company1.getTaxIdentificationNumber())

        then: "returned taxCalculation is accurate"
        taxCalculation.getIncome() == BigDecimal.valueOf(4200)
        taxCalculation.getCosts() == BigDecimal.valueOf(2000)
        taxCalculation.getIncomeMinusCosts() == BigDecimal.valueOf(2200)
        taxCalculation.getIncomingVat() == BigDecimal.valueOf(966)
        taxCalculation.getOutgoingVat() == BigDecimal.valueOf(460)
        taxCalculation.getVatToReturn() == BigDecimal.valueOf(506)
        taxCalculation.getPensionInsurance() == BigDecimal.valueOf(500)
        taxCalculation.getIncomeMinusCostsMinusPensionInsurance() == BigDecimal.valueOf(1700)
        taxCalculation.getTaxCalculationBase() == BigDecimal.valueOf(1700)
        taxCalculation.getIncomeTax() == BigDecimal.valueOf(323)
        taxCalculation.getHealthInsurance9() == BigDecimal.valueOf(90)
        taxCalculation.getHealthInsurance775() == BigDecimal.valueOf(77.5)
        taxCalculation.getIncomeTaxMinusHealthInsurance() == BigDecimal.valueOf(245.5)
        taxCalculation.getFinalIncomeTaxValue() == BigDecimal.valueOf(245)
    }

    def "should calculate tax with personal car expenses"() {
        given: "database with invoices with personal car entries"
        addInvoicesWithPersonalCarEntries()

        when: "we ask taxCalculatorService to calculate taxes"
        TaxCalculation taxCalculation = taxCalculatorService.getTaxCalculation(company1.getTaxIdentificationNumber())

        then: "returned taxCalculation is accurate"
        taxCalculation.getIncome() == BigDecimal.valueOf(4200)
        taxCalculation.getCosts() == BigDecimal.valueOf(2138)
        taxCalculation.getIncomeMinusCosts() == BigDecimal.valueOf(2062)
        taxCalculation.getIncomingVat() == BigDecimal.valueOf(966)
        taxCalculation.getOutgoingVat() == BigDecimal.valueOf(322)
        taxCalculation.getVatToReturn() == BigDecimal.valueOf(644)
        taxCalculation.getPensionInsurance() == BigDecimal.valueOf(500)
        taxCalculation.getIncomeMinusCostsMinusPensionInsurance() == BigDecimal.valueOf(1562)
        taxCalculation.getTaxCalculationBase() == BigDecimal.valueOf(1562)
        taxCalculation.getIncomeTax() == BigDecimal.valueOf(296.78)
        taxCalculation.getHealthInsurance9() == BigDecimal.valueOf(90)
        taxCalculation.getHealthInsurance775() == BigDecimal.valueOf(77.5)
        taxCalculation.getIncomeTaxMinusHealthInsurance() == BigDecimal.valueOf(219.28)
        taxCalculation.getFinalIncomeTaxValue() == BigDecimal.valueOf(219)
    }

    def "should throw NoSuchElementException when tax id is not in database"() {
        when:"we ask taxCalculatorService to calculate taxes"
        taxCalculatorService.getTaxCalculation(company1.getTaxIdentificationNumber())

        then: "NoSuchElementException is thrown"
        thrown(NoSuchElementException)
    }

    void addInvoicesWithPersonalCarEntries() {
        Company company2 = CompanyFixture.getCompany()
        Company c1 = companyRepository.save(company1)
        Company c2 = companyRepository.save(company2)
        Invoice invoice1 = new Invoice(UUID.randomUUID(), "number1", LocalDate.now(), c1, c2, InvoiceEntryFixture.getInvoiceEntryListWithPersonalCar(6))
        Invoice invoice2 = new Invoice(UUID.randomUUID(), "number2", LocalDate.now(), c2, c1, InvoiceEntryFixture.getInvoiceEntryListWithPersonalCar(4))
        invoiceRepository.save(invoice1)
        invoiceRepository.save(invoice2)
    }

    void addInvoicesWithoutPersonalCarEntries() {
        Company company2 = CompanyFixture.getCompany()
        Company c1 = companyRepository.save(company1)
        Company c2 = companyRepository.save(company2)
        Invoice invoice1 = new Invoice(UUID.randomUUID(), "number1", LocalDate.now(), c1, c2, InvoiceEntryFixture.getInvoiceEntryListWithoutPersonalCar(6))
        Invoice invoice2 = new Invoice(UUID.randomUUID(), "number2", LocalDate.now(), c2, c1, InvoiceEntryFixture.getInvoiceEntryListWithoutPersonalCar(4))
        invoiceRepository.save(invoice1)
        invoiceRepository.save(invoice2)
    }

    void clearDatabase() {
        deleteInvoices()
        deleteCompanies()
    }

    void deleteInvoices() {
        invoiceRepository.deleteAll()
    }

    void deleteCompanies() {
        companyRepository.deleteAll()
    }
}
