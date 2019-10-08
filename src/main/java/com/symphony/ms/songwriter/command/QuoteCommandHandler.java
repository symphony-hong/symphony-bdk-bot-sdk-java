package com.symphony.ms.songwriter.command;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.symphony.ms.songwriter.internal.command.CommandHandler;
import com.symphony.ms.songwriter.internal.event.model.MessageEvent;
import com.symphony.ms.songwriter.internal.lib.restclient.RestClient;
import com.symphony.ms.songwriter.internal.lib.restclient.model.RestResponse;
import com.symphony.ms.songwriter.internal.message.model.SymphonyMessage;

public class QuoteCommandHandler extends CommandHandler {

  private RestClient restClient;
  private String quoteRequest = "https://www.alphavantage.co/query?function=CURRENCY_EXCHANGE_RATE&from_currency=USD&to_currency=%s&apikey=C7G0Q2QOJ80OECGM";

  public QuoteCommandHandler(RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  protected Predicate<String> getCommandMatcher() {
    return Pattern
        .compile("^@"+ getBotName() + " \\/rate")
        .asPredicate();
  }

  @Override
  public void handle(MessageEvent command, SymphonyMessage commandResponse) {
    String currency = command.getMessage().split(" \\/rate ")[1];

    if (currency != null) {
      RestResponse<QuoteResponse> response = restClient.getRequest(String.format(quoteRequest, currency), QuoteResponse.class);

      if (response.getStatus() == 200) {

        QuoteResponse test = (QuoteResponse)response.getBody();

        InternalQuote iQuote = new InternalQuote(test.getQuote());

        commandResponse.setEnrichedTemplateFile("quote-result.ftl", iQuote, "com.symphony.ms.devtools.currencyQuote", iQuote, "1.0");
      }
    }

  }

}
