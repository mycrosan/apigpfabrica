package br.compneusgppremium.api.message;

import java.util.List;

public class ResponseMessage {
    private String message;



  private List content;

    public ResponseMessage(String message, List content) {
        this.message = message;
        this.content = content;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

  public List getContent() {
    return content;
  }

  public void setContent(List content) {
    this.content = content;
  }

}
