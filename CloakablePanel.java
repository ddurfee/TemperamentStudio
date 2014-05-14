/************************

KeyboardPanel.java

This class is part of
TemperamentStudio
(c) 2014 Dallin S. Durfee
This code may be modified and redistributed
under the MIT license

This class creates a panel that 
retains its size when invisible

******************/

import javax.swing.JPanel;
import javax.swing.Box;
import java.awt.Component;
import javax.swing.border.Border;
import javax.swing.BorderFactory;
import java.awt.Dimension;
import java.awt.Insets;

public class CloakablePanel extends JPanel{

    private Component spacer=null;
    private Border theborder = null;
    private int xm,ym;
    private Boolean iscloaked;
    
    CloakablePanel(int extraXMargin, int extraYMargin){
	xm = extraXMargin;
	ym = extraYMargin;
	// 10 for flowlayout
	iscloaked = false;
    }

    public void cloak(){
	if(iscloaked == false){
	    iscloaked = true;
	    theborder = getBorder();
	    //uncloak();
	    //Insets insets = getInsets();
	    //spacer = Box.createRigidArea(new Dimension(getWidth() - insets.left - insets.right -xm, getHeight() - insets.top - insets.bottom -ym));
	    spacer = Box.createRigidArea(new Dimension(getWidth() - xm, getHeight() - ym));
	    Component[] components = getComponents();
	    for(int i=0; i < components.length; i++){
		components[i].setVisible(false);
	    }
	    add(spacer);
	    setBorder(javax.swing.BorderFactory.createEmptyBorder());
	}
    }
    
    public void uncloak(){
	if(iscloaked == true){
	    iscloaked = false;
	    if(spacer != null)
		remove(spacer);
	    Component[] components = getComponents();
	    for(int i=0; i < components.length; i++){
		components[i].setVisible(true);
	    }
	    if(theborder != null)
		setBorder(theborder);
	}
    }

}
