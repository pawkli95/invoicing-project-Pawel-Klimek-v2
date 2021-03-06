import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CompanyService } from 'src/app/services/company.service';
import { CompanyDto } from 'src/app/dto/company-dto';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-company',
  templateUrl: './company.component.html',
  styleUrls: ['./company.component.scss']
})
export class CompanyComponent implements OnInit {

  constructor(private activatedRoute: ActivatedRoute, private companyService: CompanyService, private router: Router,
     private toastr: ToastrService) { 
    this.id = this.activatedRoute.snapshot.paramMap.get('id')
  }

  id: string | null 
  item: CompanyDto = {
    "id": '',
  "taxIdentificationNumber": '',
  "name": '',
  "address": '',
  "pensionInsurance": 0,
  "healthInsurance": 0
  }

  formGroup: FormGroup = new FormGroup( {
    taxIdentificationNumber: new FormControl('', [Validators.required]),
    name: new FormControl('', [Validators.required]),
    address: new FormControl('', [Validators.required]),
    pensionInsurance: new FormControl('', [Validators.required, Validators.pattern("^[0-9]+(.[0-9]{0,2})?$")]),
    healthInsurance: new FormControl('', [Validators.required, Validators.pattern("^[0-9]+(.[0-9]{0,2})?$")])
  }
  )

  ngOnInit(): void {
    if(!!this.id)
    this.companyService.getCompany(this.id).subscribe(data => {
      this.item = data
      this.formGroup.patchValue({
        ...data
      })
    }, error => {
      console.log(error)
    } )
  }

  public update(): void {
    this.companyService.updateCompany({
      ...this.formGroup.value,
      id: this.item.id
    }).subscribe(() => {
      this.toastr.success("Updated succesfully")
    }, error => {
      console.log(error)
    }, () => {
      this.goBack()
    })
    

  }

  public goBack(): void {
    this.router.navigate(['companies'])
  }

  public goToInvoices(taxId: string) {
    this.router.navigate(['companies', 'invoices', taxId])
  }

  get name() {
    return this.formGroup.get('name') as FormControl
  }

  get address() {
    return this.formGroup.get('address') as FormControl
  }

  get pension() {
    return this.formGroup.get('pensionInsurance') as FormControl
  }

  get health() {
    return this.formGroup.get('healthInsurance') as FormControl
  }




  

}
