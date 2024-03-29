//License
/***
 * Java TelnetD library (embeddable telnet daemon)
 * Copyright (c) 2000-2005 Dieter Wimberger 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the author nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER AND CONTRIBUTORS ``AS
 * IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 ***/

package net.wimpi.telnetd.io.toolkit;

import net.wimpi.telnetd.io.BasicTerminalIO;

import java.io.IOException;

/**
 * Class that implements an Editfield.
 *
 * @author Dieter Wimberger
 * @version 2.0 (13/03/2005)
 */
public class Editfield
    extends ActiveComponent {

  //Associations
  private InputFilter m_InputFilter = null;
  private InputValidator m_InputValidator = null;
  //Aggregations (inner class!)
  private Buffer m_Buffer;
  //Members
  private int m_Cursor = 0;
  private boolean m_InsertMode = true;
  private int m_LastSize = 0;
  private boolean m_PasswordField = false;
  private boolean m_JustBackspace;

  /**
   * Constructs an Editfield.
   */
  public Editfield(BasicTerminalIO io, String name, int length) {
    //init superclass
    super(io, name);
    //init class params
    m_Buffer = new Buffer(length);
    setDimension(new Dimension(length, 1));
    m_Cursor = 0;
    m_InsertMode = true;
  }//constructor

  /**
   * Accessor method for field length.
   *
   * @return int that represents length of editfield.
   */
  public int getLength() {
    return m_Dim.getWidth();
  }//getLength

  /**
   * Accessor method for field buffer size.
   *
   * @return int that represents the number of chars in the fields buffer.
   */
  public int getSize() {
    return m_Buffer.size();
  }//getSize

  public String getValue() {
    return m_Buffer.toString();
  }//getValue

  public void setValue(String str)
      throws BufferOverflowException, IOException {
    clear();
    append(str);
  }//setValue

  public void clear() throws IOException {
    positionCursorAtBegin();
    for (int i = 0; i < m_Buffer.size(); i++) {
      m_IO.write(' ');
    }
    positionCursorAtBegin();
    m_Buffer.clear();
    m_Cursor = 0;
    m_LastSize = 0;
    m_IO.flush();
  }//clear

  public char getCharAt(int pos)
      throws IndexOutOfBoundsException {

    return m_Buffer.getCharAt(pos);
  }//getCharAt

  public void setCharAt(int pos, char ch)
      throws IndexOutOfBoundsException, IOException {

    //buffer
    m_Buffer.setCharAt(pos, ch);
    //cursor
    //implements overwrite mode no change
    //screen
    draw();
  }//setCharAt

  public void insertCharAt(int pos, char ch)
      throws BufferOverflowException, IndexOutOfBoundsException, IOException {

    storeSize();
    //buffer
    m_Buffer.ensureSpace(1);
    m_Buffer.insertCharAt(pos, ch);
    //cursor adjustment (so that it stays in "same" pos)
    if (m_Cursor >= pos) {
      moveRight();
    }
    //screen
    draw();
  }//insertCharAt

  public void removeCharAt(int pos)
      throws IndexOutOfBoundsException, IOException {

    storeSize();
    //buffer
    m_Buffer.removeCharAt(pos);
    //cursor adjustment
    if (m_Cursor > pos) {
      moveLeft();
    }
    //screen
    draw();
  }//removeChatAt

  public void insertStringAt(int pos, String str)
      throws BufferOverflowException, IndexOutOfBoundsException, IOException {

    storeSize();
    //buffer
    m_Buffer.ensureSpace(str.length());
    for (int i = 0; i < str.length(); i++) {
      m_Buffer.insertCharAt(pos, str.charAt(i));
      //Cursor
      m_Cursor++;
    }
    //screen
    draw();

  }//insertStringAt

  public void append(char ch)
      throws BufferOverflowException, IOException {

    storeSize();
    //buffer
    m_Buffer.ensureSpace(1);
    m_Buffer.append(ch);
    //cursor
    m_Cursor++;
    //screen
    if (!m_PasswordField) {
      m_IO.write(ch);
    } else {
      m_IO.write('.');
    }
  }//append(char)

  public void append(String str)
      throws BufferOverflowException, IOException {

    storeSize();
    //buffer
    m_Buffer.ensureSpace(str.length());
    for (int i = 0; i < str.length(); i++) {
      m_Buffer.append(str.charAt(i));
      //Cursor
      m_Cursor++;
    }
    //screen
    if (!m_PasswordField) {
      m_IO.write(str);
    } else {
      StringBuffer sbuf = new StringBuffer();
      for (int n = 0; n < str.length(); n++) {
        sbuf.append('.');
      }
      m_IO.write(sbuf.toString());
    }
  }//append(String)

  public int getCursorPosition() {
    return m_Cursor;
  }//getCursorPosition

  public boolean isJustBackspace() {
    return m_JustBackspace;
  }//isJustBackspace

  public void setJustBackspace(boolean b) {
    m_JustBackspace = true;
  }//setJustBackspace

  /**
   * @param filter Object instance that implements the InputFilter interface.
   */
  public void registerInputFilter(InputFilter filter) {
    m_InputFilter = filter;
  }//registerInputFilter

  /**
   * @param validator Object instance that implements the InputValidator interface.
   */
  public void registerInputValidator(InputValidator validator) {
    m_InputValidator = validator;
  }//registerInputValidator

  public boolean isInInsertMode() {
    return m_InsertMode;
  }//isInInsertMode

  public void setInsertMode(boolean b) {
    m_InsertMode = b;
  }//setInsertMode

  public boolean isPasswordField() {
    return m_PasswordField;
  }//isPasswordField

  public void setPasswordField(boolean b) {
    m_PasswordField = b;
  }//setPasswordField

  /**
   * Method that will be
   * reading and processing input.
   */
  public void run() throws IOException {
    int in = 0;
    //m_IO.setAutoflushing(false);
    draw();
    m_IO.flush();
    do {
      //get next key
      in = m_IO.read();
      //Just backspace mode, convert deletes to backspace
      if (m_JustBackspace && in == BasicTerminalIO.DELETE) {
        in = BasicTerminalIO.BACKSPACE;
      }
      //send it through the filter if one is set
      if (m_InputFilter != null) {
        in = m_InputFilter.filterInput(in);
      }
      switch (in) {
        case -1:
          m_Buffer.clear();
          break;
        case InputFilter.INPUT_HANDLED:
          continue;
        case InputFilter.INPUT_INVALID:
          m_IO.bell();
          break;
        case BasicTerminalIO.LEFT:
          moveLeft();
          break;
        case BasicTerminalIO.RIGHT:
          moveRight();
          break;
        case BasicTerminalIO.UP:
        case BasicTerminalIO.DOWN:
          m_IO.bell();
          break;
        case BasicTerminalIO.ENTER:
          if (m_InputValidator != null) {
            if (m_InputValidator.validate(m_Buffer.toString())) {
              in = -1;
            } else {
              m_IO.bell();
            }
          } else {
            in = -1;
          }
          break;
        case BasicTerminalIO.BACKSPACE:
          try {
            removeCharAt(m_Cursor - 1);
          } catch (IndexOutOfBoundsException ioobex) {
            m_IO.bell();
          }
          break;
        case BasicTerminalIO.DELETE:
          try {
            removeCharAt(m_Cursor);
          } catch (IndexOutOfBoundsException ioobex) {
            m_IO.bell();
          }
          break;
        case BasicTerminalIO.TABULATOR:
          in = -1;
          break;
        default:
          handleCharInput(in);
      }
      m_IO.flush();
    } while (in != -1);
  }//run


  public void draw() throws IOException {
    //System.out.println("DEBUG: Buffer="+ m_Buffer.toString());
    //System.out.println("DEBUG: Cursor="+ m_Cursor);

    int diff = m_LastSize - m_Buffer.size();
    String output = m_Buffer.toString();
    if (m_PasswordField) {
      StringBuffer stbuf = new StringBuffer();
      for (int n = 0; n < output.length(); n++) {
        stbuf.append('.');
      }
      output = stbuf.toString();
    }
    //System.out.println("DEBUG: Sizediff="+diff);
    if (diff > 0) {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append(output);
      for (int i = 0; i < diff; i++) {
        sbuf.append(" ");
      }
      output = sbuf.toString();
    }

    if (m_Position != null) {
      m_IO.setCursor(m_Position.getRow(), m_Position.getColumn());
    } else {
      m_IO.moveLeft(m_Cursor);
    }
    m_IO.write(output);
    //adjust screen cursor hmm
    if (m_Cursor < output.length()) {
      m_IO.moveLeft(output.length() - m_Cursor);
    }
  }//draw

  private void moveRight() throws IOException {
    //cursor
    if (m_Cursor < m_Buffer.size()) {
      m_Cursor++;
      //screen
      m_IO.moveRight(1);
    } else {
      m_IO.bell();
    }
  }//moveRight

  private void moveLeft() throws IOException {
    //cursor
    if (m_Cursor > 0) {
      m_Cursor--;
      //screen
      m_IO.moveLeft(1);
    } else {
      m_IO.bell();
    }
  }//moveLeft

  private void positionCursorAtBegin() throws IOException {
    //1. position cursor at first char
    if (m_Position == null) {
      m_IO.moveLeft(m_Cursor);
    } else {
      m_IO.setCursor(m_Position.getRow(), m_Position.getColumn());
    }
  }//positionCursoratBegin

  private boolean isCursorAtEnd() {
    return (m_Cursor == m_Buffer.size());
  }//isCursorAtEnd

  private void handleCharInput(int ch) throws IOException {
    if (isCursorAtEnd()) {
      try {
        //Field
        append((char) ch);
      } catch (BufferOverflowException bex) {
        m_IO.bell();
      }
    } else {
      if (isInInsertMode()) {
        try {
          //Field
          insertCharAt(m_Cursor, (char) ch);
        } catch (BufferOverflowException bex) {
          m_IO.bell();
        }
      } else {
        try {
          //Field
          setCharAt(m_Cursor, (char) ch);
        } catch (IndexOutOfBoundsException bex) {
          m_IO.bell();
        }
      }
    }
  }//handleCharInput

  private void storeSize() {
    m_LastSize = m_Buffer.size();
  }//storeSize


  //inner class Buffer
  class Buffer extends CharBuffer {

    public Buffer(int size) {
      super(size);
    }//constructor

  }//class Buffer

}//class Editfield
