package com.confApi.wooba.sales;

import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.wooba.sales.dto.WoobaSalesDetailsResponse;
import com.confApi.wooba.webhook.WoobaWebhookRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WoobaAirReservationMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WoobaAirReservationMapper mapper = new WoobaAirReservationMapper();

    @Test
    void deveMapearStatusReservaPelosTransactionStatesDaWooba() throws Exception {
        assertEquals(1, mapReservaComStatus(2, "Reserved").getStatus());
        assertEquals(3, mapReservaComStatus(4, "Issued").getStatus());
        assertEquals(2, mapReservaComStatus(5, "Canceled").getStatus());
    }

    @Test
    void deveMarcarReservaParaIgnorarQuandoContextCustomerVierPreenchido() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "Transaction": {
                    "Header": {
                      "TransactionType": 1,
                      "TransactionTypeDescription": "AirReservation",
                      "UniqueId": "AIR-CUSTOMER",
                      "Locator": "AVP5AG",
                      "TransactionState": 2,
                      "TransactionStateDescription": "Reserved"
                    },
                    "Context": {
                      "Agency": {
                        "Id": 3204
                      },
                      "Customer": {
                        "Id": 13659,
                        "Name": "TREVO OPERACIONAL"
                      }
                    },
                    "User": {
                      "Username": "cnf.cris"
                    },
                    "ProductDetail": {
                      "AirReservationDetail": {
                        "Airline": {
                          "Code": "G3"
                        }
                      }
                    }
                  }
                }
                """);

        WoobaSalesDetailsResponse details = new WoobaSalesDetailsResponse();
        details.setSuccess(true);
        details.setTransaction(root.path("Transaction"));

        ReservaAereo reserva = mapper.toReservaAereo(details, null);

        assertNotNull(reserva.getRegraReserva());
        assertTrue(reserva.getRegraReserva().contains("WoobaCustomer=true"));
        assertTrue(reserva.getRegraReserva().contains("WoobaCustomerId=13659"));
        assertTrue(reserva.getRegraReserva().contains("WoobaCustomerName=TREVO OPERACIONAL"));
    }

    @Test
    void naoDeveMarcarCustomerQuandoContextCustomerVierNull() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "Transaction": {
                    "Header": {
                      "TransactionType": 1,
                      "TransactionTypeDescription": "AirReservation",
                      "UniqueId": "AIR-SEM-CUSTOMER",
                      "Locator": "AVP5AG",
                      "TransactionState": 2,
                      "TransactionStateDescription": "Reserved"
                    },
                    "Context": {
                      "Agency": {
                        "Id": 3204
                      },
                      "Customer": null
                    },
                    "User": {
                      "Username": "cnf.cris"
                    },
                    "ProductDetail": {
                      "AirReservationDetail": {
                        "Airline": {
                          "Code": "G3"
                        }
                      }
                    }
                  }
                }
                """);

        WoobaSalesDetailsResponse details = new WoobaSalesDetailsResponse();
        details.setSuccess(true);
        details.setTransaction(root.path("Transaction"));

        ReservaAereo reserva = mapper.toReservaAereo(details, null);

        assertNotNull(reserva.getRegraReserva());
        assertTrue(!reserva.getRegraReserva().contains("WoobaCustomer=true"));
    }

    @Test
    void devePopularDatasDaReservaAtivaComLastUpdateComOffset() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "Transaction": {
                    "Header": {
                      "TransactionType": 1,
                      "TransactionTypeDescription": "AirReservation",
                      "UniqueId": "AIR-RESERVED",
                      "Locator": "AVP5AG",
                      "LastUpdate": "2026-06-17T11:43:24.8154967-03:00",
                      "TransactionState": 2,
                      "TransactionStateDescription": "Reserved"
                    },
                    "Context": {
                      "Agency": {
                        "Id": 3204
                      },
                      "Customer": null
                    },
                    "User": {
                      "Username": "cnf.cris"
                    },
                    "ProductDetail": {
                      "AirReservationDetail": {
                        "Airline": {
                          "Code": "G3"
                        }
                      }
                    }
                  }
                }
                """);

        WoobaSalesDetailsResponse details = new WoobaSalesDetailsResponse();
        details.setSuccess(true);
        details.setTransaction(root.path("Transaction"));

        ReservaAereo reserva = mapper.toReservaAereo(details, null);

        assertEquals(1, reserva.getStatus());
        assertNotNull(reserva.getDataCriacao());
        assertNotNull(reserva.getDataLimiteEmissao());
        assertNull(reserva.getDataEmissao());
    }

    @Test
    void deveGarantirDatasObrigatoriasMesmoSemDatasDaWooba() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "Transaction": {
                    "Header": {
                      "TransactionType": 1,
                      "TransactionTypeDescription": "AirReservation",
                      "UniqueId": "AIR-SEM-DATAS",
                      "Locator": "AVP5AG",
                      "TransactionState": 2,
                      "TransactionStateDescription": "Reserved"
                    },
                    "Context": {
                      "Agency": {
                        "Id": 3204
                      },
                      "Customer": null
                    },
                    "User": {
                      "Username": "cnf.cris"
                    },
                    "ProductDetail": {
                      "AirReservationDetail": {
                        "Airline": {
                          "Code": "G3"
                        }
                      }
                    }
                  }
                }
                """);

        WoobaSalesDetailsResponse details = new WoobaSalesDetailsResponse();
        details.setSuccess(true);
        details.setTransaction(root.path("Transaction"));

        ReservaAereo reserva = mapper.toReservaAereo(details, null);

        assertEquals(1, reserva.getStatus());
        assertNotNull(reserva.getDataCriacao());
        assertNotNull(reserva.getDataLimiteEmissao());
        assertNull(reserva.getDataEmissao());
    }

    @Test
    void deveManterDataEmissaoNoCancelamentoDeBilhete() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "Transaction": {
                    "Header": {
                      "TransactionType": 100,
                      "TransactionTypeDescription": "AirTicket",
                      "Locator": "LUVAQT",
                      "Ticket": "1272305924504",
                      "LastUpdate": "2026-06-15T16:21:27.697",
                      "TransactionState": 5,
                      "TransactionStateDescription": "Canceled"
                    },
                    "Context": {
                      "Agency": {
                        "Id": 1872
                      }
                    },
                    "User": {
                      "Username": "cnf.cris"
                    },
                    "ProductDetail": {
                      "AirTicketDetail": {
                        "IssueDate": "2026-06-15T00:00:00",
                        "Passengers": [
                          {
                            "Ticket": "1272305924504",
                            "Person": {
                              "FirstName": "JOAO",
                              "Surname": "SILVA"
                            }
                          }
                        ]
                      }
                    }
                  }
                }
                """);

        WoobaSalesDetailsResponse details = new WoobaSalesDetailsResponse();
        details.setSuccess(true);
        details.setTransaction(root.path("Transaction"));

        ReservaAereo reserva = mapper.toReservaAereo(details, null);

        assertEquals(2, reserva.getStatus());
        assertNotNull(reserva.getDataEmissao());
        assertNotNull(reserva.getDataCancelamento());
        assertEquals(0, reserva.getPassageiros().get(0).getBilhetes().get(0).getStatus());
        assertNotNull(reserva.getPassageiros().get(0).getBilhetes().get(0).getDataEmissao());
        assertNotNull(reserva.getPassageiros().get(0).getBilhetes().get(0).getDataCancelamento());
    }

    @Test
    void deveCancelarPagamentoQuandoTransacaoOuLinkEstiverCancelado() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "Transaction": {
                    "Header": {
                      "TransactionType": 100,
                      "TransactionTypeDescription": "AirTicket",
                      "Locator": "LUVAQT",
                      "Ticket": "1272305924502",
                      "LastUpdate": "2026-06-15T16:21:50.13",
                      "TransactionState": 5,
                      "TransactionStateDescription": "Canceled"
                    },
                    "Context": {
                      "Agency": {
                        "Id": 1872
                      }
                    },
                    "User": {
                      "Username": "cnf.cris"
                    },
                    "Payments": [
                      {
                        "PaymentType": 1,
                        "PaymentTypeDescription": "Customer",
                        "PaymentFormDescription": "Invoice",
                        "Amount": 2113.10,
                        "TransactionAmount": 2113.10,
                        "PaymentState": 4,
                        "PaymentStateDescription": "Confirmed",
                        "Date": "2026-06-15T00:00:00.001",
                        "Links": [
                          {
                            "TransactionTypeDescription": "AirTicket",
                            "UniqueId": "TKT-80409FAB-E5C3-4267-ADBA-B22BFA9DF8F0",
                            "Ticket": "1272305924502",
                            "TransactionState": 5,
                            "TransactionStateDescription": "Canceled"
                          }
                        ]
                      }
                    ],
                    "ProductDetail": {
                      "AirTicketDetail": {
                        "IssueDate": "2026-06-15T00:00:00",
                        "Passengers": [
                          {
                            "Ticket": "1272305924502",
                            "Person": {
                              "FirstName": "JOAO",
                              "Surname": "SILVA"
                            }
                          }
                        ]
                      }
                    }
                  }
                }
                """);

        WoobaSalesDetailsResponse details = new WoobaSalesDetailsResponse();
        details.setSuccess(true);
        details.setTransaction(root.path("Transaction"));

        ReservaAereo reserva = mapper.toReservaAereo(details, null);

        assertEquals(1, reserva.getRecebimentos().size());
        assertEquals(0, reserva.getRecebimentos().get(0).getStatus());
        assertEquals(2113.10, reserva.getRecebimentos().get(0).getValrCancelado(), 0.001);
        assertTrue(reserva.getRecebimentos().get(0).getMensagem().contains("WoobaPaymentTicket=1272305924502"));
    }

    @Test
    void deveMapearValorDuPorDescricaoOuTipoMonetarioDaWooba() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "Transaction": {
                    "Header": {
                      "TransactionType": 100,
                      "TransactionTypeDescription": "AirTicket",
                      "Locator": "JLEZAB",
                      "Ticket": "1272305930937",
                      "LastUpdate": "2026-06-15T17:28:21.213",
                      "TransactionState": 4,
                      "TransactionStateDescription": "Issued"
                    },
                    "Context": {
                      "Agency": {
                        "Id": 1872
                      }
                    },
                    "User": {
                      "Username": "cnf.cris"
                    },
                    "ProductDetail": {
                      "AirTicketDetail": {
                        "IssueDate": "2026-06-15T00:00:00",
                        "Passengers": [
                          {
                            "Ticket": "1272305930937",
                            "Person": {
                              "FirstName": "MARIA",
                              "Surname": "SILVA"
                            },
                            "MonetaryDetail": [
                              {
                                "MonetaryDetailTypeDescription": "Sale",
                                "MonetaryInfo": [
                                  {
                                    "CurrencyCode": "BRL",
                                    "Amount": 597.06,
                                    "MonetaryInfoType": 15,
                                    "MonetaryInfoTypeDescription": "Total"
                                  },
                                  {
                                    "CurrencyCode": "BRL",
                                    "Amount": 512.90,
                                    "MonetaryInfoType": 20,
                                    "MonetaryInfoTypeDescription": "FarePrice"
                                  },
                                  {
                                    "CurrencyCode": "BRL",
                                    "Amount": 51.29,
                                    "MonetaryInfoType": 4,
                                    "MonetaryInfoTypeDescription": "Du"
                                  }
                                ]
                              }
                            ]
                          }
                        ]
                      }
                    }
                  }
                }
                """);

        WoobaSalesDetailsResponse details = new WoobaSalesDetailsResponse();
        details.setSuccess(true);
        details.setTransaction(root.path("Transaction"));

        ReservaAereo reserva = mapper.toReservaAereo(details, null);

        assertEquals(51.29, reserva.getPassageiros().get(0).getReservaValores().get(0).getValorDu(), 0.001);
    }

    @Test
    void deveMapearRavEFeeParaReservaValor() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "Transaction": {
                    "Header": {
                      "TransactionType": 1,
                      "TransactionTypeDescription": "AirReservation",
                      "Locator": "ATGGMA",
                      "LastUpdate": "2026-06-16T15:02:46.787",
                      "TransactionState": 2,
                      "TransactionStateDescription": "Reserved"
                    },
                    "Context": {
                      "Agency": {
                        "Id": 1872
                      }
                    },
                    "User": {
                      "Username": "cnf.cris"
                    },
                    "ProductDetail": {
                      "AirReservationDetail": {
                        "Airline": {
                          "Code": "DL"
                        },
                        "Passengers": [
                          {
                            "Person": {
                              "FirstName": "CRISTIAN",
                              "Surname": "CAVAZZINI"
                            },
                            "MonetaryDetail": [
                              {
                                "MonetaryDetailTypeDescription": "Sale",
                                "MonetaryInfo": [
                                  {
                                    "CurrencyCode": "BRL",
                                    "Amount": 3328.38,
                                    "MonetaryInfoType": 20,
                                    "MonetaryInfoTypeDescription": "FarePrice"
                                  },
                                  {
                                    "CurrencyCode": "BRL",
                                    "Amount": 232.99,
                                    "MonetaryInfoType": 5,
                                    "MonetaryInfoTypeDescription": "Rav"
                                  },
                                  {
                                    "CurrencyCode": "BRL",
                                    "Amount": 151.29,
                                    "MonetaryInfoType": 6,
                                    "MonetaryInfoTypeDescription": "Fee"
                                  },
                                  {
                                    "CurrencyCode": "USD",
                                    "Amount": 30.0,
                                    "MonetaryInfoType": 6,
                                    "MonetaryInfoTypeDescription": "Fee"
                                  }
                                ]
                              }
                            ]
                          }
                        ]
                      }
                    }
                  }
                }
                """);

        WoobaSalesDetailsResponse details = new WoobaSalesDetailsResponse();
        details.setSuccess(true);
        details.setTransaction(root.path("Transaction"));

        ReservaAereo reserva = mapper.toReservaAereo(details, null);

        assertEquals(232.99, reserva.getPassageiros().get(0).getReservaValores().get(0).getValorRav(), 0.001);
        assertEquals(151.29, reserva.getPassageiros().get(0).getReservaValores().get(0).getValorRc(), 0.001);
        assertNull(reserva.getPassageiros().get(0).getReservaValores().get(0).getValorDu());
    }

    @Test
    void deveMapearPagamentoConfirmadoComCartao() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "Transaction": {
                    "Header": {
                      "TransactionType": 100,
                      "TransactionTypeDescription": "AirTicket",
                      "Locator": "LA9579915CZRW",
                      "Ticket": "9572291986020",
                      "LastUpdate": "2026-06-16T16:15:54.103",
                      "TransactionState": 4,
                      "TransactionStateDescription": "Issued"
                    },
                    "Context": {
                      "Agency": {
                        "Id": 1872
                      }
                    },
                    "User": {
                      "Username": "cnf.cris"
                    },
                    "Payments": [
                      {
                        "Id": 0,
                        "PaymentType": 2,
                        "PaymentTypeDescription": "Provider",
                        "PaymentForm": 101,
                        "PaymentFormDescription": "Invoice",
                        "CurrencyCode": "BRL",
                        "Amount": 438.07,
                        "TransactionAmount": 438.07,
                        "PaymentState": 4,
                        "PaymentStateDescription": "Confirmed",
                        "Date": "2026-06-16T00:00:00"
                      },
                      {
                        "Id": 11889166,
                        "PaymentType": 1,
                        "PaymentTypeDescription": "Customer",
                        "PaymentForm": 105,
                        "PaymentFormDescription": "CreditCard",
                        "CurrencyCode": "BRL",
                        "Amount": 481.24,
                        "TransactionAmount": 481.24,
                        "PaymentState": 4,
                        "PaymentStateDescription": "Confirmed",
                        "Date": "2026-06-16T16:15:14.197",
                        "MerchantTransactionId": "ARZRW1781637313",
                        "ProviderTransactionId": "b84e6cfe-5cdb-4794-abd6-b46b010bdd37",
                        "CreditCard": {
                          "BrandCode": "VI",
                          "BrandDescription": "Visa",
                          "CardNumber": "422007XXXXXX2015",
                          "Holder": "CRISTIAN CAVAZZINI",
                          "ExpirationDate": "09/2030",
                          "Installments": 2,
                          "FirstInstallmentAmount": 240.62,
                          "OtherInstallmentsAmount": 240.62,
                          "AuthorizationCode": "672YOS",
                          "ProofOfSale": "253024897",
                          "PaymentAccountReference": "10472606161613275850",
                          "POS": {
                            "Id": 2,
                            "Name": "LATAM C",
                            "MerchantId": 82960828
                          }
                        },
                        "Links": [
                          {
                            "TransactionType": 100,
                            "TransactionTypeDescription": "AirTicket",
                            "UniqueId": "TKT-F2664FE7-FC59-4646-AD98-86BB62440ADC",
                            "Locator": "LA9579915CZRW",
                            "Ticket": "9572291986020"
                          }
                        ],
                        "Register": {
                          "Id": 35,
                          "Name": "Cartao NDC",
                          "BackofficeDetail": {
                            "Code": "CCC"
                          }
                        }
                      }
                    ],
                    "ProductDetail": {
                      "AirReservationDetail": null,
                      "AirTicketDetail": {
                        "Airline": {
                          "Code": "LA"
                        },
                        "Passengers": []
                      }
                    }
                  }
                }
                """);

        WoobaSalesDetailsResponse details = new WoobaSalesDetailsResponse();
        details.setSuccess(true);
        details.setTransaction(root.path("Transaction"));

        ReservaAereo reserva = mapper.toReservaAereo(details, null);

        assertEquals(1, reserva.getRecebimentos().size());
        Recebimento recebimento = reserva.getRecebimentos().get(0);
        assertEquals(481.24, recebimento.getValrRecebimento(), 0.001);
        assertEquals(1, recebimento.getStatus());
        assertEquals("CARTAO", recebimento.getCodgFormaPagto().getNomeFormaPagto());
        assertEquals(2, recebimento.getQtdeParcela());
        assertEquals(240.62, recebimento.getValrPrimeiraParcela(), 0.001);
        assertEquals(240.62, recebimento.getValrDemaisParcela(), 0.001);
        assertEquals("422007XXXXXX2015", recebimento.getNumrCartao());
        assertEquals("09/2030", recebimento.getValidadeCartao());
        assertEquals("CRISTIAN CAVAZZINI", recebimento.getTitularCartao());
        assertEquals("672YOS", recebimento.getCodgAutCartao());
        assertEquals("ARZRW1781637313", recebimento.getCodgTransacao());
        assertEquals("11889166", recebimento.getOrderGatewayCartao());
        assertTrue(recebimento.getMensagem().contains("WoobaPaymentUniqueId=TKT-F2664FE7-FC59-4646-AD98-86BB62440ADC"));
        assertTrue(recebimento.getMensagem().contains("WoobaRegisterCode=CCC"));
        assertTrue(recebimento.getMensagem().contains("WoobaCardBrandCode=VI"));
        assertTrue(recebimento.getMensagem().contains("WoobaCardBrand=Visa"));
        assertTrue(recebimento.getMensagem().contains("WoobaCardProofOfSale=253024897"));
        assertTrue(recebimento.getMensagem().contains("WoobaCardPosName=LATAM C"));
    }

    @Test
    void deveNormalizarCompanhiaLatamJjParaLa() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "Transaction": {
                    "Header": {
                      "TransactionType": 100,
                      "TransactionTypeDescription": "AirTicket",
                      "Locator": "LA9573438SQYY",
                      "Ticket": "9572291848001",
                      "LastUpdate": "2026-06-15T18:02:20.373",
                      "TransactionState": 4,
                      "TransactionStateDescription": "Issued"
                    },
                    "Context": {
                      "Agency": {
                        "Id": 1872
                      }
                    },
                    "User": {
                      "Username": "cnf.cris"
                    },
                    "ProductDetail": {
                      "AirTicketDetail": {
                        "IssueDate": "2026-06-15T00:00:00",
                        "Airline": {
                          "Code": "JJ"
                        },
                        "Flights": [
                          {
                            "DepartureAirportCode": "GRU",
                            "ArrivalAirportCode": "GIG",
                            "MarketingAirline": {
                              "Code": "LA"
                            },
                            "FlightNumber": "3342"
                          }
                        ],
                        "Passengers": [
                          {
                            "Ticket": "9572291848001",
                            "Person": {
                              "FirstName": "FERNANDA",
                              "Surname": "PARRADO"
                            }
                          }
                        ]
                      }
                    }
                  }
                }
                """);

        WoobaSalesDetailsResponse details = new WoobaSalesDetailsResponse();
        details.setSuccess(true);
        details.setTransaction(root.path("Transaction"));

        ReservaAereo reserva = mapper.toReservaAereo(details, null);

        assertEquals("LA", reserva.getCodgCompanhiaAerea().getIataCia());
        assertEquals("LA", reserva.getTrechos().get(0).getCodgCompanhiaAerea().getIataCia());
        assertEquals("LA", reserva.getTrechos().get(0).getVoos().get(0).getCodgCompanhiaAerea().getIataCia());
    }

    @Test
    void devePopularReservaAereaComDetailsDaWooba() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "Transaction": {
                    "Header": {
                      "TransactionType": 1,
                      "TransactionTypeDescription": "AirReservation",
                      "Id": 8659119,
                      "UniqueId": "AIR-0CC27A59-76C0-4006-8AD2-41E75A253943",
                      "Locator": "3FKMU6",
                      "LastUpdate": "2022-07-18T16:49:17.607",
                      "TransactionState": 4,
                      "TransactionStateDescription": "Issued"
                    },
                    "Context": {
                      "Agency": {
                        "Id": 10,
                        "Name": "Nome Agencia",
                        "Document": {
                          "Number": "00000000/000000"
                        },
                        "BackofficeDetail": {
                          "Code": "000.0"
                        }
                      }
                    },
                    "User": {
                      "Id": 65002,
                      "Name": "DENIS LIMA",
                      "Username": "denis.wooba",
                      "Email": "denis.lima@wooba.com.br",
                      "Document": {
                        "Number": "00000083000"
                      }
                    },
                    "ProductDetail": {
                      "InsertDate": "2022-07-18T16:43:15.247",
                      "AirReservationDetail": {
                        "Deadline": "2022-07-19T10:00:00",
                        "RouteDescription": "LIS/MAD",
                        "MonetaryDetail": [
                          {
                            "MonetaryDetailTypeDescription": "Sale",
                            "MonetaryInfo": [
                              {
                                "CurrencyCode": "BRL",
                                "Amount": 381.49,
                                "MonetaryInfoTypeDescription": "Total"
                              },
                              {
                                "CurrencyCode": "BRL",
                                "Amount": 147.60,
                                "MonetaryInfoTypeDescription": "FarePrice"
                              },
                              {
                                "CurrencyCode": "BRL",
                                "Amount": 213.56,
                                "MonetaryInfoTypeDescription": "Tax"
                              },
                              {
                                "CurrencyCode": "BRL",
                                "Amount": 10.33,
                                "MonetaryInfoTypeDescription": "Rav"
                              },
                              {
                                "CurrencyCode": "BRL",
                                "Amount": 10.00,
                                "MonetaryInfoTypeDescription": "Fee"
                              }
                            ]
                          }
                        ],
                        "Passengers": [
                          {
                            "TypeCode": "ADT",
                            "Email": "denis.lima@wooba.com.br",
                            "Person": {
                              "FirstName": "DENIS",
                              "Surname": "LIMA",
                              "Gender": 1,
                              "GenderDescription": "Male"
                            }
                          }
                        ],
                        "Flights": [
                          {
                            "DepartureAirportCode": "LIS",
                            "DepartureDate": "2022-08-26T00:00:00",
                            "DepartureTime": "22:00",
                            "ArrivalAirportCode": "MAD",
                            "ArrivalDate": "2022-08-27T00:00:00",
                            "ArrivalTime": "00:20",
                            "MarketingAirline": {
                              "Code": "TP"
                            },
                            "CodeShare": false,
                            "FlightNumber": "1022",
                            "ClassOfService": "T",
                            "FareBasisCode": "T04BSC0A",
                            "Brand": {
                              "Code": "BASIC",
                              "Name": "BASIC"
                            },
                            "EquipmentType": "320",
                            "Baggage": {
                              "Quantity": 1,
                              "MaximumWeight": 0
                            },
                            "AirlineLocator": "3FKMU6"
                          }
                        ]
                      }
                    }
                  }
                }
                """);

        WoobaSalesDetailsResponse details = new WoobaSalesDetailsResponse();
        details.setSuccess(true);
        details.setTransaction(root.path("Transaction"));

        WoobaWebhookRequest webhook = new WoobaWebhookRequest();
        webhook.setUniqueId("AIR-0CC27A59-76C0-4006-8AD2-41E75A253943");
        webhook.setLocator("3FKMU6");

        ReservaAereo reserva = mapper.toReservaAereo(details, webhook);

        assertEquals("3FKMU6", reserva.getLocalizador());
        assertEquals(3, reserva.getStatus());
        assertNotNull(reserva.getDataCriacao());
        assertNotNull(reserva.getDataEmissao());
        assertEquals(2, reserva.getCodgSistema().getCodgSistema());
        assertEquals(2, reserva.getFonte());
        assertEquals("TP", reserva.getCodgCompanhiaAerea().getIataCia());
        assertEquals(10, reserva.getCodgAgencia().getIdWoobaAgencia());
        assertEquals(65002, reserva.getCodgUsuarioCriacao().getCodigoWooba());
        assertEquals(381.49, reserva.getValorTotalReserva(), 0.001);
        assertEquals(1, reserva.getPassageiros().size());
        assertEquals("DENIS", reserva.getPassageiros().get(0).getNomePassageiro());
        assertEquals(147.60, reserva.getPassageiros().get(0).getReservaValores().get(0).getValorTarifa(), 0.001);
        assertEquals(1, reserva.getTrechos().size());
        assertEquals("1022", reserva.getTrechos().get(0).getVoos().get(0).getNumeroVoo());
        assertEquals("HK", reserva.getTrechos().get(0).getVoos().get(0).getStatusVoo());
        assertEquals("TP", reserva.getTrechos().get(0).getVoos().get(0).getCodgCompanhiaAereaOperada().getIataCia());
        assertEquals("LIS", reserva.getTrechos().get(0).getCodgAeroportoOrigem().getIataAeroporto());
    }

    @Test
    void devePopularReservaAereaComAirTicketDetailDaWooba() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "Transaction": {
                    "Header": {
                      "TransactionType": 100,
                      "TransactionTypeDescription": "AirTicket",
                      "Id": 11264222,
                      "UniqueId": "TKT-E7FE7059-5DC0-4234-850C-668E99A67148",
                      "Locator": "GXNHDJ",
                      "Ticket": "7242614288032",
                      "LastUpdate": "2026-05-25T10:19:04.387",
                      "TransactionState": 4,
                      "TransactionStateDescription": "Issued"
                    },
                    "Context": {
                      "Agency": {
                        "Id": 15055,
                        "Name": "JPA VIAGENS",
                        "Document": {
                          "Type": 2,
                          "TypeDescription": "Cnpj",
                          "Number": "04432768/000195"
                        },
                        "BackofficeDetail": {
                          "Code": "600745",
                          "CostumerCode": "600745",
                          "WoofficeCode": ""
                        },
                        "Iata": ""
                      }
                    },
                    "User": {
                      "Id": 54287,
                      "Name": "MATEUS RALPH SANCHEZ VAILANT",
                      "Username": "mateus.cwb",
                      "Email": "mateus.ralph@confiancaturismo.com.br",
                      "Document": {
                        "Type": 1,
                        "TypeDescription": "Cpf",
                        "Number": "06539268122"
                      }
                    },
                    "Payments": [
                      {
                        "Id": 0,
                        "PaymentType": 1,
                        "PaymentTypeDescription": "Customer",
                        "PaymentForm": 101,
                        "PaymentFormDescription": "Invoice",
                        "Ticket": null,
                        "CurrencyCode": "BRL",
                        "Amount": 11655.88,
                        "TransactionAmount": 11655.88,
                        "PaymentState": 4,
                        "PaymentStateDescription": "Confirmed",
                        "Date": "2026-05-25T00:00:00.001",
                        "MerchantTransactionId": null,
                        "ProviderTransactionId": null,
                        "Links": [
                          {
                            "TransactionType": 100,
                            "TransactionTypeDescription": "AirTicket",
                            "UniqueId": "TKT-E7FE7059-5DC0-4234-850C-668E99A67148",
                            "Locator": "GXNHDJ",
                            "Ticket": "7242614288032"
                          }
                        ],
                        "Register": {
                          "Id": 43,
                          "Name": "Prazo especial",
                          "Description": "Prazo especial",
                          "BackofficeDetail": {
                            "Code": "FAT"
                          }
                        }
                      },
                      {
                        "Id": 0,
                        "PaymentType": 2,
                        "PaymentTypeDescription": "Provider",
                        "PaymentForm": 101,
                        "PaymentFormDescription": "Invoice",
                        "CurrencyCode": "BRL",
                        "Amount": 11655.88,
                        "TransactionAmount": 11655.88,
                        "PaymentState": 4,
                        "PaymentStateDescription": "Confirmed"
                      }
                    ],
                    "Links": [
                      {
                        "TransactionType": 1,
                        "TransactionTypeDescription": "AirReservation",
                        "UniqueId": "AIR-EE81B79E-051D-463B-96CA-DA1D2A27D48B",
                        "Locator": "GXNHDJ"
                      }
                    ],
                    "ProductDetail": {
                      "InsertDate": "2026-05-25T10:13:06.34",
                      "AirReservationDetail": null,
                      "AirTicketDetail": {
                        "Flights": [
                          {
                            "DepartureAirportCode": "LDB",
                            "DepartureDate": "2026-06-20T00:00:00",
                            "DepartureTime": "10:05",
                            "ArrivalAirportCode": "GRU",
                            "ArrivalTime": "11:15",
                            "ArrivalDate": "2026-06-20T00:00:00",
                            "MarketingAirline": {
                              "Code": "LX"
                            },
                            "CodeShare": "false",
                            "OperationAirline": null,
                            "FlightNumber": "9740",
                            "ClassOfService": "H",
                            "FareBasisCode": "HX0XJYNC",
                            "EquipmentType": "320",
                            "Baggage": {
                              "Quantity": 0,
                              "MaximumWeight": 0.0
                            },
                            "AirlineLocator": "GXNHDJ"
                          },
                          {
                            "DepartureAirportCode": "GRU",
                            "DepartureDate": "2026-06-20T00:00:00",
                            "DepartureTime": "18:25",
                            "ArrivalAirportCode": "ZRH",
                            "ArrivalTime": "10:40",
                            "ArrivalDate": "2026-06-21T00:00:00",
                            "MarketingAirline": {
                              "Code": "LX"
                            },
                            "FlightNumber": "0093",
                            "ClassOfService": "H",
                            "FareBasisCode": "HX0XJYNC",
                            "EquipmentType": "77W",
                            "Baggage": {
                              "Quantity": 0,
                              "MaximumWeight": 0.0
                            },
                            "AirlineLocator": "GXNHDJ"
                          },
                          {
                            "DepartureAirportCode": "ZRH",
                            "DepartureDate": "2026-06-21T00:00:00",
                            "DepartureTime": "12:25",
                            "ArrivalAirportCode": "VIE",
                            "ArrivalTime": "13:45",
                            "ArrivalDate": "2026-06-21T00:00:00",
                            "MarketingAirline": {
                              "Code": "LX"
                            },
                            "FlightNumber": "3534",
                            "ClassOfService": "H",
                            "FareBasisCode": "HX0XJYNC",
                            "EquipmentType": "320",
                            "Baggage": {
                              "Quantity": 0,
                              "MaximumWeight": 0.0
                            },
                            "AirlineLocator": "GXNHDJ"
                          },
                          {
                            "DepartureAirportCode": "BEG",
                            "DepartureDate": "2026-06-26T00:00:00",
                            "DepartureTime": "17:10",
                            "ArrivalAirportCode": "FRA",
                            "ArrivalTime": "19:10",
                            "ArrivalDate": "2026-06-26T00:00:00",
                            "MarketingAirline": {
                              "Code": "LH"
                            },
                            "FlightNumber": "1413",
                            "ClassOfService": "Q",
                            "FareBasisCode": "QX0XJYNC",
                            "EquipmentType": "32N",
                            "Baggage": {
                              "Quantity": 0,
                              "MaximumWeight": 0.0
                            },
                            "AirlineLocator": "B2NS5S"
                          },
                          {
                            "DepartureAirportCode": "FRA",
                            "DepartureDate": "2026-06-26T00:00:00",
                            "DepartureTime": "22:05",
                            "ArrivalAirportCode": "GRU",
                            "ArrivalTime": "04:50",
                            "ArrivalDate": "2026-06-27T00:00:00",
                            "MarketingAirline": {
                              "Code": "LH"
                            },
                            "FlightNumber": "0506",
                            "ClassOfService": "Q",
                            "FareBasisCode": "QX0XJYNC",
                            "EquipmentType": "74H",
                            "Baggage": {
                              "Quantity": 0,
                              "MaximumWeight": 0.0
                            },
                            "AirlineLocator": "B2NS5S"
                          },
                          {
                            "DepartureAirportCode": "GRU",
                            "DepartureDate": "2026-06-27T00:00:00",
                            "DepartureTime": "07:40",
                            "ArrivalAirportCode": "LDB",
                            "ArrivalTime": "08:55",
                            "ArrivalDate": "2026-06-27T00:00:00",
                            "MarketingAirline": {
                              "Code": "LA"
                            },
                            "FlightNumber": "4531",
                            "ClassOfService": "M",
                            "FareBasisCode": "QX0XJYNC",
                            "EquipmentType": "320",
                            "Baggage": {
                              "Quantity": 0,
                              "MaximumWeight": 0.0
                            },
                            "AirlineLocator": "JTYNBX"
                          }
                        ],
                        "IssueDate": "2026-05-25T00:00:00",
                        "Route": 2,
                        "RouteDescription": "International",
                        "FareBasisCode": "HX0XJYNC",
                        "Airline": {
                          "Code": "LX"
                        },
                        "Passengers": [
                          {
                            "Person": {
                              "FirstName": "RODRIGO MR",
                              "Surname": "MARQUES DA SILVA",
                              "Gender": 1,
                              "GenderDescription": "Male"
                            },
                            "Email": null,
                            "TypeCode": "ADT",
                            "ProviderId": "3",
                            "Ticket": "7242614288032",
                            "MonetaryDetail": [
                              {
                                "MonetaryDetailTypeDescription": "Sale",
                                "MonetaryInfo": [
                                  {
                                    "CurrencyCode": "BRL",
                                    "Amount": 11655.88,
                                    "MonetaryInfoTypeDescription": "Total"
                                  },
                                  {
                                    "CurrencyCode": "BRL",
                                    "Amount": 9967.83,
                                    "MonetaryInfoTypeDescription": "FarePrice"
                                  },
                                  {
                                    "CurrencyCode": "BRL",
                                    "Amount": 1801.3,
                                    "MonetaryInfoTypeDescription": "Rav"
                                  },
                                  {
                                    "Code": "DU",
                                    "Description": "DU",
                                    "CurrencyCode": "BRL",
                                    "Amount": 109.71
                                  },
                                  {
                                    "CurrencyCode": "BRL",
                                    "Amount": 109.70,
                                    "MonetaryInfoTypeDescription": "MkpFarePrice"
                                  },
                                  {
                                    "CurrencyCode": "BRL",
                                    "Amount": 8.98,
                                    "MonetaryInfoTypeDescription": "MkpDu"
                                  },
                                  {
                                    "CurrencyCode": "BRL",
                                    "Amount": 839.88,
                                    "MonetaryInfoTypeDescription": "Tax"
                                  }
                                ]
                              }
                            ]
                          }
                        ],
                        "MonetaryDetail": [
                          {
                            "MonetaryDetailTypeDescription": "Sale",
                            "MonetaryInfo": [
                              {
                                "CurrencyCode": "BRL",
                                "Amount": 11655.88,
                                "MonetaryInfoTypeDescription": "Total"
                              }
                            ]
                          }
                        ],
                        "ConjunctiveTicket": [
                          "7242614288033"
                        ],
                        "DepartureAirportCode": "LDB",
                        "ArrivalAirportCode": "LDB"
                      }
                    }
                  }
                }
                """);

        WoobaSalesDetailsResponse details = new WoobaSalesDetailsResponse();
        details.setSuccess(true);
        details.setTransaction(root.path("Transaction"));

        WoobaWebhookRequest webhook = new WoobaWebhookRequest();
        webhook.setTransactionType(100);
        webhook.setUniqueId("TKT-E7FE7059-5DC0-4234-850C-668E99A67148");
        webhook.setLocator("GXNHDJ");
        webhook.setTicket("7242614288032");

        ReservaAereo reserva = mapper.toReservaAereo(details, webhook);

        assertEquals("GXNHDJ", reserva.getLocalizador());
        assertEquals(3, reserva.getStatus());
        assertNotNull(reserva.getDataCriacao());
        assertNotNull(reserva.getDataEmissao());
        assertNotNull(reserva.getDataLimiteEmissao());
        assertEquals("LX", reserva.getCodgCompanhiaAerea().getIataCia());
        assertEquals(2, reserva.getFonte());
        assertEquals(15055, reserva.getCodgAgencia().getIdWoobaAgencia());
        assertEquals("600745", reserva.getCodgAgencia().getCodgSistemaBackOffice());
        assertEquals("mateus.cwb", reserva.getCodgUsuarioCriacao().getLoginUsuario());
        assertEquals(54287, reserva.getCodgUsuarioCriacao().getCodigoWooba());
        assertTrue(reserva.getRegraReserva().contains("WoobaAirUniqueId=AIR-EE81B79E-051D-463B-96CA-DA1D2A27D48B"));
        assertEquals(11655.88, reserva.getValorTotalReserva(), 0.001);
        assertEquals(1, reserva.getPassageiros().size());
        assertEquals("RODRIGO MR", reserva.getPassageiros().get(0).getNomePassageiro());
        assertEquals("MARQUES DA SILVA", reserva.getPassageiros().get(0).getSobrenomePassageiro());
        assertEquals("3", reserva.getPassageiros().get(0).getIdPassageiroCia());
        assertEquals(2, reserva.getPassageiros().get(0).getBilhetes().size());
        assertEquals("7242614288032", reserva.getPassageiros().get(0).getBilhetes().get(0).getNumrBilhete());
        assertEquals(1, reserva.getPassageiros().get(0).getBilhetes().get(0).getStatus());
        assertNotNull(reserva.getPassageiros().get(0).getBilhetes().get(0).getDataEmissao());
        assertEquals("7242614288033", reserva.getPassageiros().get(0).getBilhetes().get(1).getNumrBilhete());
        assertEquals(9967.83, reserva.getPassageiros().get(0).getReservaValores().get(0).getValorTarifa(), 0.001);
        assertEquals(839.88, reserva.getPassageiros().get(0).getReservaValores().get(0).getValorTaxaEmbarque(), 0.001);
        assertEquals(1801.3, reserva.getPassageiros().get(0).getReservaValores().get(0).getValorRav(), 0.001);
        assertEquals(109.71, reserva.getPassageiros().get(0).getReservaValores().get(0).getValorDu(), 0.001);
        assertEquals(118.68, reserva.getPassageiros().get(0).getReservaValores().get(0).getValorMkp(), 0.001);
        assertEquals(1, reserva.getRecebimentos().size());
        assertEquals(11655.88, reserva.getRecebimentos().get(0).getValrRecebimento(), 0.001);
        assertEquals(1, reserva.getRecebimentos().get(0).getStatus());
        assertEquals("FATURADO", reserva.getRecebimentos().get(0).getCodgFormaPagto().getNomeFormaPagto());
        assertEquals(1, reserva.getRecebimentos().get(0).getQtdeParcela());
        assertNotNull(reserva.getRecebimentos().get(0).getDataRecebimento());
        assertTrue(reserva.getRecebimentos().get(0).getMensagem().contains("WoobaRegisterCode=FAT"));
        assertEquals(6, reserva.getTrechos().size());
        assertEquals("9740", reserva.getTrechos().get(0).getVoos().get(0).getNumeroVoo());
        assertEquals("LDB", reserva.getTrechos().get(0).getCodgAeroportoOrigem().getIataAeroporto());
        assertEquals("GRU", reserva.getTrechos().get(0).getCodgAeroportoDestino().getIataAeroporto());
    }

    private ReservaAereo mapReservaComStatus(int transactionState, String transactionStateDescription) throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "Transaction": {
                    "Header": {
                      "TransactionState": %d,
                      "TransactionStateDescription": "%s",
                      "Locator": "STATUS1"
                    },
                    "Context": {
                      "Agency": {
                        "Id": 1872
                      }
                    },
                    "User": {
                      "Username": "teste"
                    },
                    "ProductDetail": {
                      "AirReservationDetail": {}
                    }
                  }
                }
                """.formatted(transactionState, transactionStateDescription));

        WoobaSalesDetailsResponse details = new WoobaSalesDetailsResponse();
        details.setSuccess(true);
        details.setTransaction(root.path("Transaction"));

        return mapper.toReservaAereo(details, null);
    }
}
