package main

import (
	"encoding/json"
	"fmt"
	"log"
	"models"
	"util"

	"github.com/fragmenta/query"
	"github.com/gin-gonic/gin"
	_ "github.com/mattn/go-sqlite3"
)

func main() {
	r := gin.Default()
	options := map[string]string{"adapter": "sqlite3", "db": "./catalog.sqlite"}
	err := query.OpenDatabase(options)
	if err != nil {
		log.Fatal(err.Error())
		return
	}
	r.POST("/operators", func(c *gin.Context) {
		var operator models.Operator
		err := json.NewDecoder(c.Request.Body).Decode(&operator)
		log.Println(operator)
		if err != nil {
			log.Println("Error: " + err.Error())
			err = c.AbortWithError(400, err)
			return
		} else {
			_, err := operator.Create()
			if err != nil {
				log.Println("Error: " + err.Error())
				err = c.AbortWithError(400, err)
				return
			} else {
				err = util.SendNewOperatorEvent(operator.Id)
				if err != nil {
					_ = c.AbortWithError(400, err)
				} else {
					c.JSON(200, operator)
				}
			}

		}
	})
	r.DELETE("/operators/:operatorId", func(c *gin.Context) {
		operatorId := c.Param("operatorId")
		operator := models.NewOperator()
		operator.Id = operatorId
		err := operator.Delete()
		if err != nil {
			log.Println("Error: " + err.Error())
			err = c.AbortWithError(400, err)
			return
		} else {
			c.Status(200)
			return;
		}
	})
	r.POST("/operators/:operatorId/services", func(c *gin.Context) {
		operatorId := c.Param("operatorId")
		var service models.Service
		err := json.NewDecoder(c.Request.Body).Decode(&service)
		if err != nil {
			log.Println("Error: " + err.Error())
			err = c.AbortWithError(400, err)
			return
		}
		operator := models.NewOperator()
		operator.Id = operatorId
		err = operator.AddService(service)
		if err != nil {
			log.Println("Error: " + err.Error())
			err = c.AbortWithError(400, err)
			return
		} else {
			c.JSON(200, service)
		}

	})
	r.GET("/operators/:operatorId/services", func(c *gin.Context) {
		operatorId := c.Param("operatorId")
		services, err := models.GetServices(operatorId)
		if err != nil {
			log.Println("Error: " + err.Error())
			err = c.AbortWithError(400, err)
			return
		} else {
			if services == nil {
				services = []*models.Service{}
			}
			code := 200
			if len(services) == 0 {
				err = c.AbortWithError(404, fmt.Errorf("operator or services rip"))
			} else {
				c.JSON(code, services)
			}
		}

	})
	r.GET("/operators", func(c *gin.Context) {
		operators, err := models.FetchOperators()
		if err != nil {
			log.Println("Error: " + err.Error())
			return
		} else {
			c.JSON(200, operators)
		}
	})
	r.GET("/products", func(c *gin.Context) {
		c.JSON(200, gin.H{
			"products": gin.H{
				"metro": []gin.H{
					{
						"type":  "flat-fare",
						"price": 1000,
					},
					{
						"type":   "ci-co",
						"metric": "time",
						"price":  50,
					},
				},
				"ttsl": []gin.H{
					{
						"type":  "flat-fare",
						"price": 2000,
					},
				},
				"taxify": []gin.H{
					{
						"type": "operatorDefinedValue",
					},
				},
			},
		})
	})
	serverError := r.Run("0.0.0.0:8081") // listen and serve on 0.0.0.0:8080
	if serverError != nil {
		log.Fatal("Server isn't running")
	}
}
