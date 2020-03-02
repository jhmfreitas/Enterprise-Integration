package models

import (
	"github.com/fragmenta/query"
	"log"
)

type Address struct {
	District string `json:"district"`
	County   string `json:"county"`
	ZipCode  string `json:"zip_code"`
	Name     string `json:"client"`
}

func (o *Address) Create() (int64, error) {
	var cols = make(map[string]string)
	cols["district"] = o.District
	cols["county"] = o.County
	cols["zip_code"] = o.ZipCode
	cols["name"] = o.Name
	return OperatorQuery().Insert(cols)
}

func AddressQuery() *query.Query {
	return query.New("address", "zip_code")
}

func MakeAddressFromCols(cols map[string]interface{}) *Address {
	address := NewAddress()
	if cols["zip_code"] != nil {
		address.ZipCode = cols["zip_code"].(string)
	}
	if cols["name"] != nil {
		address.Name = cols["name"].(string)
	}
	if cols["district"] != nil {
		address.District = cols["district"].(string)
	}
	if cols["county"] != nil {
		address.County = cols["county"].(string)
	}

	return address
}

func GetAddress(zipCode string) (*Address, error) {
	result, err := AddressQuery().Where("zip_code=?", zipCode).FirstResult()
	log.Println(result)
	if err != nil {
		return nil, err
	} else {
		return MakeAddressFromCols(result), nil
	}
}

func NewAddress() *Address {
	return &Address{}
}
