package pl.futurecollars.invoicing.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.futurecollars.invoicing.dto.TaxCalculation;
import pl.futurecollars.invoicing.service.TaxCalculatorService;

@Api(tags = {"tax-calculator-controller"})
@Slf4j
@RestController
@RequestMapping("/api/tax")
@AllArgsConstructor
@CrossOrigin(origins = "http://localhost:4200/")
public class TaxCalculatorController {

    private final TaxCalculatorService taxCalculatorService;

    @ApiOperation(value = "Get tax calculation")
    @GetMapping("/{taxId}")
    public ResponseEntity<TaxCalculation> getTaxCalculation(@PathVariable String taxId) {
        log.debug("Getting tax calculation");
        return ResponseEntity.ok().body(taxCalculatorService.getTaxCalculation(taxId));
    }
}
