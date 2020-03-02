package util

import (
	"context"
	"encoding/json"
	"time"
)
import "github.com/segmentio/kafka-go"
func sendMessage (inputMessages [][]byte) error{
	topic := "maas"
	partition := 0

	conn, _ := kafka.DialLeader(context.Background(), "tcp", "100.25.186.5:9092", topic, partition)

	err := conn.SetWriteDeadline(time.Now().Add(10*time.Second))
	if err != nil {
		return err
	}
	var messages = make([]kafka.Message, len(inputMessages))
	for _, r := range inputMessages {
		messages = append(messages, kafka.Message{Value: r})
	}
	_, err = conn.WriteMessages(messages...)
	_ = conn.Close()
	return err
}
// to produce messages

func SendNewOperatorEvent(operatorId string) error{
	eventType := "new-operator"
	jsonString, err := json.Marshal(NewOperatorEvent{
		Type:eventType,
		Operator:operatorId,
	})
	if err != nil {
		return err
	}
	return sendMessage([][]byte{jsonString})
}

type NewOperatorEvent struct{
	Type string `json:"type"`
	Operator string `json:"operator"`
}
