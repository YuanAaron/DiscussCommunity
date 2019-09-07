package com.oshacker.discusscommunity.utils;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger LOGGER= LoggerFactory.getLogger(SensitiveFilter.class);

    //替换符
    private static final String REPLACEMENT="***";

    //读取敏感词
    @PostConstruct
    public void init() {
        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
            //InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("sensitive-words.txt");
            BufferedReader br=new BufferedReader(new InputStreamReader(is));
            String lineTxt;
            while((lineTxt=br.readLine())!=null) {
                //添加到Trie
                addKeyword(lineTxt);
            }
        } catch (IOException e) {
            LOGGER.error("读取敏感词文件失败"+e.getMessage());
        }
    }

    //Trie节点的数据结构
    private class TrieNode {
        //是不是关键词的结尾
        private boolean isKeywordEnd=false;

        //当前结点的所有子节点，比如ab、ac、ad
        private Map<Character,TrieNode> subNodes=new HashMap<>();
    }
    private TrieNode root=new TrieNode();

    //将一个关键词添加到Trie中
    private void addKeyword(String lineTxt) {
        TrieNode cur=root;
        for (int i=0;i<lineTxt.length();i++) {
            Character c=lineTxt.charAt(i);
            //c对应的子节点
            TrieNode subNode=cur.subNodes.get(c);

            //判断是否要新建节点（比如，在已经添加敏感词abc的前提下，再添加敏感词abe，就无需再创建节点a和b）
            if (subNode==null) {
                subNode=new TrieNode();
                cur.subNodes.put(c,subNode);
            }

            cur=subNode;//（曾经因为这里的空指针找了很久）

            //设置关键词结尾的标识也可以放在这里，但我更喜欢放在最后
//            if (i==lineTxt.length()-1) {
//                cur.isKeywordEnd=true;
//            }
        }
        //此时cur指向敏感词的最后一个字符对应的节点

        cur.isKeywordEnd=true; //设置关键词结尾的标识
    }

    //敏感词过滤逻辑
    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }

        StringBuilder sb=new StringBuilder();
        TrieNode cur=root; //指针1:指向TrieNode
        int begin=0; //指针2
        int pos=0; //指针3
        while (begin<text.length()) {
            char c=text.charAt(pos);

            //跳过符号
            if (isSymbol(c)) {
                //若指针1处于根节点，将此符号加入sb中，指针2向下移一步
                if (cur==root) {
                    sb.append(c);
                    begin++;
                }
                //无论符号在开头或中间，指针3都向下移一步
                pos++;
                continue;
            }

            cur=cur.subNodes.get(c);
            if (cur==null) { //以begin开头的字符串不是敏感词
                sb.append(text.charAt(begin));
                pos=++begin;
                cur=root; //cur重新指向根节点
            } else if (cur.isKeywordEnd) {//发现敏感词
                //将begin~pos字符串替换掉
                sb.append(REPLACEMENT);
                begin=++pos;
                cur=root;
            } else {//是不是敏感词还不确定（处在检查中）
                //已知敏感词有fabcd和abc,要检验的字符串的最后一段为fabc, 此时指针2指f,
                //指针3指到c, 根据循环中的判断c的idKeywordEnd为false, pos++, 此时跳出循环,
                //然后将fabc加到sb中, 但是abc这个敏感词没有被过滤掉。
                if (pos<text.length()-1) {
                    pos++;
                }
            }
        }

        return sb.toString();
    }

    //判断是否为符号（a-zA-z0-9以及汉字之外）
    private boolean isSymbol(char c) {
        int intc=(int) c;
        //0x2E80~0x9FFF表示东亚文字
        return !CharUtils.isAsciiAlphanumeric(c)&&(intc<0x2E80||intc>0x9FFF);
    }
}