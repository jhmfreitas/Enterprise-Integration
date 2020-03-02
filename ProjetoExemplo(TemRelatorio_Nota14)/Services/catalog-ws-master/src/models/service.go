package models

import (
	"fmt"
	"github.com/fragmenta/query"
)

type Service struct {
	Id string `json:"name"`
	OperatorId string `json:"operator_id"`
	ServiceType string `json:"service_type"`
	Tariff int64 `json:"tariff"`
}

func (service *Service) Create() error {
	cols := make(map[string]string)
	cols["operator_id"] = service.OperatorId
	cols["id"] = service.Id
	cols["tariff"] = fmt.Sprintf("%d",service.Tariff)
	cols["service_type_id"] = service.ServiceType
	_, err := ServiceQuery().Insert(cols)
	return err
}

func DeleteOperatorServices(operatorId string) error {
	return ServiceQuery().Where("operator_id=?", operatorId).DeleteAll()
}

func (service *Service) Delete() error {
	return ServiceQuery().Where("id=?", service.Id).Delete()
}

func NewServiceFromCols(cols map[string]interface{}) *Service {
	service := NewService()
	if cols["id"] != nil {
		service.Id = cols["id"].(string)
	}
	if cols["operator_id"] != nil {
		service.OperatorId = cols["operator_id"].(string)
	}
	if cols["service_type_id"] != nil {
		service.ServiceType = cols["service_type_id"].(string)
	}
	if cols["tariff"] != nil {
		service.Tariff = cols["tariff"].(int64)
	}
 	return service
}

func ServiceFromQueryResults(results []query.Result) ([]*Service, error) {
	var result []*Service
	for _, r := range results {
		m := NewServiceFromCols(r)
		result = append(result, m)
	}
	return result, nil
}

func ServiceQuery() *query.Query {
	return query.New("service", "service_pk")
}

// New initialises and returns a new Service
func NewService() *Service {
	service := &Service{}
	return service
}