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
import java.util.Vector;


/**
 * Class implementing a selection menu.
 *
 * @author Dieter Wimberger
 * @version 2.0 (13/03/2005)
 */
public class Selection extends ActiveComponent {

  //Members & Associations
  private Vector m_Options;
  private int m_Selected;
  private int m_LastSelected;

  /**
   * Constructs a Selection instance.
   *
   * @param io   Object instance implementing the BasicTerminalIO interface.
   * @param name String representing this instances name.
   */
  public Selection(BasicTerminalIO io, String name) {
    super(io, name);
    m_Options = new Vector(10, 5);
    m_LastSelected = 0;
    m_Selected = 0;
  }//constructor

  /**
   * Method to add an Option to a Selection instance.
   *
   * @param str String representing the option.
   */
  public void addOption(String str) {
    m_Options.addElement(str);
  }//addOption

  /**
   * Method to insert an Option to a Selection instance at a specific
   * index. Falls back to add, if index is corrupt.
   *
   * @param str   String representing the option.
   * @param index int representing the desired index.
   */
  public void insertOption(String str, int index) {
    try {
      m_Options.insertElementAt(str, index);
    } catch (ArrayIndexOutOfBoundsException aex) {
      addOption(str);
    }
  }//insertOption

  /**
   * Method to remove an existing Option from a Selection instance.
   *
   * @param str String representing the option.
   */
  public void removeOption(String str) {
    for (int i = 0; i < m_Options.size(); i++) {
      if (((String) m_Options.elementAt(i)).equals(str)) {
        removeOption(i);
        return;
      }
    }
  }//removeOption

  /**
   * Method to remove an existing Option from a Selection instance.
   * Does nothing if the index is corrupt.
   *
   * @param index int representing the options index.
   */
  public void removeOption(int index) {
    try {
      m_Options.removeElementAt(index);
    } catch (ArrayIndexOutOfBoundsException aex) {
      //nothing
    }
  }//removeOption

  /**
   * Accessor method for an option of this selection.
   * Returns null if index is corrupt.
   *
   * @param index int representing the options index.
   * @return Strnig that represents the option.
   */
  public String getOption(int index) {
    try {
      Object o = m_Options.elementAt(index);
      if (o != null) {
        return (String) o;
      }
    } catch (ArrayIndexOutOfBoundsException aex) {
      //nothing
    }
    return null;
  }//getOption


  /**
   * Accessor method to retrieve the selected option.
   * Returns -1 if no option exists.
   *
   * @return index int representing index of the the selected option.
   */
  public int getSelected() {
    return m_Selected;
  }//getSelected


  /**
   * Mutator method to set selected option programatically.
   * Does nothing if the index is corrupt.
   *
   * @param index int representing an options index.
   */
  public void setSelected(int index) throws IOException {
    if (index < 0 || index > m_Options.size()) {
      return;
    } else {
      m_LastSelected = m_Selected;
      m_Selected = index;
      //needs redraw
      draw();
    }
  }//setSelected

  /**
   * Method that will make the selection active,
   * reading and processing input.
   */
  public void run() throws IOException {
    int in = 0;
    draw();
    m_IO.flush();
    do {
      //get next key
      in = m_IO.read();
      switch (in) {
        case BasicTerminalIO.LEFT:
        case BasicTerminalIO.UP:
          if (!selectPrevious()) {
            m_IO.bell();
          }
          break;
        case BasicTerminalIO.RIGHT:
        case BasicTerminalIO.DOWN:
          if (!selectNext()) {
            m_IO.bell();
          }
          break;
        case BasicTerminalIO.TABULATOR:
        case BasicTerminalIO.ENTER:
          in = -1;
          break;
        default:
          m_IO.bell();
      }
      m_IO.flush();
    } while (in != -1);

  }//run

  /**
   * Method that draws the component.
   */
  public void draw() throws IOException {

    String opttext = getOption(m_Selected);
    int diff = getOption(m_LastSelected).length() - opttext.length();

    //System.out.println("DEBUG: selected="+selected+"/"+opttext.length()+" last="+lastselected+"/"+lastlength+" diff="+diff);
    if (diff > 0) {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append(opttext);
      for (int i = 0; i < diff; i++) {
        sbuf.append(" ");
      }
      opttext = sbuf.toString();
    }

    if (m_Position != null) {
      m_IO.setCursor(m_Position.getRow(), m_Position.getColumn());
    }
    m_IO.write(opttext);
    m_IO.moveLeft(opttext.length());
  }//draw


  private boolean selectNext() throws IOException {
    if (m_Selected < (m_Options.size() - 1)) {
      setSelected(m_Selected + 1);
      return true;
    } else {
      return false;
    }
  }//selectNext


  private boolean selectPrevious() throws IOException {
    if (m_Selected > 0) {
      setSelected(m_Selected - 1);
      return true;
    } else {
      return false;
    }
  }//selectPrevious

  public static final int ALIGN_LEFT = 1;
  public static final int ALIGN_RIGHT = 2;


}//class Selection

