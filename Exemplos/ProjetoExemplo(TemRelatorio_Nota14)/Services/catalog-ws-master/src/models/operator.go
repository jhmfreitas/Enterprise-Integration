package models

import (
	"fmt"
	"github.com/fragmenta/query"
)
type Operator struct {
	Id        string `json:"id"`
	Name      string `json:"name"`
	Address   *Address `json:"address"`
	Email     string `json:"email"`
	VatNumber string `json:"vat_number"`
	ServiceTypes []string `json:"services"`
}



func FetchOperators() ([]*Operator, error) {
	results, err := OperatorQuery().Results()
	if err != nil {
		return nil, err
	} else {
		var result []*Operator
		for _, r := range results {
			m := NewOperatorFromCols(r)
			result = append(result, m)
		}
		return result, nil
	}
}

func (o *Operator) Create() (int64, error) {
	if o.Id == "" {
		return 0, fmt.Errorf("PUTO, mete id nisso")
	}
	var cols = make(map[string]string)
	cols["id"] = o.Id
	cols["name"] = o.Name
	cols["address"] = o.Address.ZipCode
	cols["email"] = o.Email
	cols["vat_number"] = o.VatNumber

	addressQuery := AddressQuery()
	count, err := addressQuery.Where("zip_code=?", o.Address.ZipCode).Count()
	if err != nil {
		return 0, err
	}
	if count == 0 {
		var address = make(map[string]string)
		address["zip_code"] = o.Address.ZipCode
		address["name"] = o.Address.Name
		address["district"] = o.Address.District
		address["county"] = o.Address.County
		res, err := addressQuery.Insert(address)
		if err != nil {
			return res, err
		}
	}

	operator, err := OperatorQuery().Insert(cols)
	operatorServiceQuery := query.New("operator_service_type", "operator_service_type_pk")
	operatorServiceTypes := make(map[string]string)
	for _, r := range o.ServiceTypes {
		operatorServiceTypes["operator_id"] = o.Id
		operatorServiceTypes["service_type_id"] = r
		res, err := operatorServiceQuery.Insert(operatorServiceTypes)
		if err != nil {
			return res, err
		}
	}
	return operator, err
}


func (o *Operator) Delete() error {
	_ = query.New("operator_service_type", "operator_service_type_pk").Where("operator_id=?", o.Id).Delete()
	_ = DeleteOperatorServices(o.Id)
	return OperatorQuery().Where("id=?", o.Id).Delete()
}

func (o *Operator) AddService(service Service) error {
	ostQuery := query.New("operator_service_type", "operator_service_type_pk")
	count, err :=ostQuery.Where("operator_id=?", o.Id).Where("service_type_id=?", service.ServiceType).Count()
	if err != nil {
		return err
	}
	if count <= 0 {
		return fmt.Errorf("service type not registered for that operator")
	}
	service.OperatorId = o.Id
	return service.Create()

}

/* ----    Util    ---- */
func OperatorQuery() *query.Query {
	return query.New("operator", "id")
}

func NewOperatorFromCols(cols map[string]interface{}) *Operator {

	operator := NewOperator()

	// Normally you'd validate col values with something like the model/validate pkg
	// we'll use a simple dummy function instead
	operator.Id = cols["id"].(string)
	if cols["name"] != nil {
		operator.Name = cols["name"].(string)
	}

	if cols["address"] != nil {
		operator.Address, _ = GetAddress(cols["address"].(string))
	}

	if cols["email"] != nil {
		operator.Email = cols["email"].(string)
	}

	if cols["vat_number"] != nil {
		operator.VatNumber = cols["vat_number"].(string)
	}
	operator.ServiceTypes = make([]string, 0)

	return operator
}

func (o *Operator) GetServices() ([]*Service, error) {
	return GetServices(o.Id)
}

func GetServices(operatorId string) ([]*Service, error) {
	servicesQuery := ServiceQuery()
	results, err := servicesQuery.Where("operator_id=?", operatorId).Results()
	if err != nil {
		return nil, nil
	} else {
		results, err := ServiceFromQueryResults(results)
		return results, err
	}
}

// New initialises and returns a new Operator
func NewOperator() *Operator {
	operator := &Operator{}
	return operator
}
