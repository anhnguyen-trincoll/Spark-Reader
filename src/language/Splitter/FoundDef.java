/* 
 * Copyright (C) 2017 Laurens Weyn
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package language.splitter;

import language.deconjugator.ValidWord;
import language.dictionary.*;
import ui.TextStream;
import ui.UI;

import java.awt.*;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;

import static ui.UI.options;

/**
 * Holds and renders a found definition of a FoundWord
 * @author laure
 */
public class FoundDef implements Comparable<FoundDef>
{
    private final ValidWord foundForm;
    private final Definition foundDef;
    
    private int defLines = 0;
    private int startLine = 0;
    
    
    public FoundDef(ValidWord foundForm, Definition foundDef)
    {
        this.foundForm = foundForm;
        this.foundDef = foundDef;
    }
    
    
    public void render(Graphics g, int xPos, int maxWidth, int y)
    {
        g.setColor(new Color(0,0,0,1));
        g.fillRect(xPos, y, maxWidth, 1);//let mouse move thorugh 1 pixel space
        y++;//slight spacer
        if(!options.getOptionBool("defsShowUpwards"))y -= g.getFontMetrics().getHeight() * 1;
        defLines = 0;//will be recounted

        //output original form if processed
        if(!foundForm.getProcess().equals(""))y = renderText(g, options.getColor("defReadingCol"), options.getColor("defBackCol"), xPos, y, foundForm.toString(), maxWidth);
        
        //output tags

        y = renderText(g, options.getColor("defTagCol"), options.getColor("defBackCol"), xPos, y, foundDef.getTagLine(), maxWidth);
        
        String[] readings = foundDef.getSpellings();
        for(String reading:readings)
        {
            if(Japanese.hasKanji(reading) && !options.getOptionBool("showAllKanji"))continue;
            //output readings if not in this form already
            if(!reading.equals(foundForm.getWord()))y = renderText(g, options.getColor("defReadingCol"), options.getColor("defBackCol"), xPos, y, reading, maxWidth);
        }
        if(!(foundDef instanceof KanjiDefinition))
        {
            for (int i = 0; i < foundForm.getWord().length(); i++)
            {
                //output Kanji if known
                char c = foundForm.getWord().charAt(i);
                String lookup = Kanji.lookup(c);
                if (lookup != null)
                {
                    y = renderText(g, options.getColor("defKanjiCol"), options.getColor("defBackCol"), xPos, y, c + "【" + lookup + "】", maxWidth);
                }
            }
        }
        for(String def:foundDef.getMeaning())
        {
            //output non-empty definitions
            if(!def.equals("") && !def.equals("(P)"))y = renderText(g, options.getColor("defCol"), options.getColor("defBackCol"), xPos, y, def, maxWidth);
        }
        
        capturePoint = 0;//disable for next iteration
    }
    
    private int capturePoint = 0;
    private String capture = "";
    /**
     * 0=disable, -1=all, y=line
     * @param pos 
     */
    public void setCapturePoint(int pos)
    {
        capturePoint = pos;
        capture = "";
    }
    public String getCapture()
    {
        return capture;
    }
    
    private int renderText(Graphics g, Color fore, Color back, int x, int y, String text, int width)
    {
        if(text == null)return y;//don't render null text
        int startY = y;
        FontMetrics font = g.getFontMetrics();
        TextStream stream = new TextStream(text);
        String line = "";
        Deque<String> lines = new LinkedList<>();
        while(!stream.isDone())
        {
            String nextBit = stream.nextWord();
            if(font.stringWidth(line + nextBit) > width)//too much for this line, wrap over
            {
                lines.add(line);//add line for rendering
                line = nextBit.trim();//put word on new line
            }
            else
            {
                line += nextBit;
            }
        }
        if(!line.equals(""))lines.add(line);//add last line
        //draw lines
        while(!lines.isEmpty())
        {
            if(options.getOptionBool("defsShowUpwards"))line = lines.pollLast();
            else line = lines.pollFirst();
            //draw line
            defLines++;
            if(startLine <= defLines)
            {
                if(options.getOptionBool("defsShowUpwards"))y -= font.getHeight();
                if(!options.getOptionBool("defsShowUpwards")) y += font.getHeight();

                //print background
                g.setColor(back);
                g.fillRect(x, y - font.getAscent(), width, font.getHeight());

                //print text
                g.setColor(fore);
                g.drawString(line, x, y);

            }
        }
        //'gap' between def lines
        g.setColor(new Color(0, 0, 0, 1));
        g.clearRect(x, y - font.getAscent() + (options.getOptionBool("defsShowUpwards")?0:font.getHeight() - 1), width,1);//clear this last line
        g.fillRect (x, y - font.getAscent() + (options.getOptionBool("defsShowUpwards")?0:font.getHeight() - 1), width,1);//add a dim line here so scrolling still works

        //capture if in this
        //TODO account for upward defs in capture
        if(capturePoint == -1 || (options.getOptionBool("defsShowUpwards")?
             (capturePoint <= startY - font.getHeight() + font.getDescent() && capturePoint > y - font.getHeight() + font.getDescent()):
             (capturePoint > startY - font.getHeight() + font.getDescent() && capturePoint <= y - font.getHeight() + font.getDescent())))
        {
            //TODO allow export with HTML color info perhaps?
            if(capture.equals(""))
            {
                capture = text;
            }
            else
            {
                capture += "\n" + text;
            }
        }
        
        return y;
    }
    public void scrollDown()
    {
        startLine = Math.min(startLine + 1, defLines - 2);
    }
    public void scrollUp()
    {
        startLine = Math.max(startLine - 1, 0);
    }
    @Override
    public String toString()
    {
        return foundForm + ": " + foundDef;
    }
    public String getFurigana()
    {
        return foundDef.getFurigana();
    }
    public String getDictForm()
    {
        return foundForm.getWord();
    }

    public Definition getDefinition()
    {
        return foundDef;
    }
    
    public int getScore() {
        int score = 0;

        if (foundDef.getSourceNum() == 1) score += 100;//prefer user added defs
        if (UI.prefDef.isPreferred(foundDef)) score += 1000;//HIGHLY favour definitions the user preferred

        if(foundDef.getSourceNum() == KanjiDefinition.SOURCENUM)score -= 500;//Kanji at back

        Set<DefTag> tags = foundDef.getTags();
        if (tags != null)
        {
            if (tags.contains(DefTag.obs) || tags.contains(DefTag.obsc) || tags.contains(DefTag.rare) || tags.contains(DefTag.arch))
                score -= 50;//obscure penalty

            if (tags.contains(DefTag.uk) && !Japanese.hasKana(foundForm.getWord())) score -= 10;//usually kana without kana
            if (tags.contains(DefTag.uK) && Japanese.hasKana(foundForm.getWord())) score -= 10;//usually Kanji, only kana

            if (tags.contains(DefTag.suf) || tags.contains(DefTag.pref))
                score -= 3;//suf/prefixes _usually_ caught with the whole word
            //TODO: only disfavour counters not attached to numbers!
            if (tags.contains(DefTag.ctr)) score -= 10;
        }
        score -= foundDef.getSpellings().length;//-1 for every spelling; more likely it's coincidence
        if (foundForm.getProcess().equals("")) score += 5;//prefer words/phrases instead of deviations


        //TODO: join numbers and counter words!
        //System.out.println("score for " + foundDef + " is " + score);
        return score;
    }

    @Override
    public int compareTo(FoundDef o)
    {
        return o.getScore() - getScore();
    }
    
    
}
