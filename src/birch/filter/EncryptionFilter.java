/*
 * EncryptionFilter.java
 *
 * Copyright (C) 2009 Beselius
 *
 * This file is part of Birch.
 *
 * Birch is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Birch is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Birch.  If not, see <http://www.gnu.org/licenses/>.
 */
package birch.filter;

import birch.AbstractFilter;
import birch.Filter;

import java.io.*;

import java.text.ParseException;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import java.security.*;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;

import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author Beselius
 */
public class EncryptionFilter extends AbstractFilter {

   public static interface Config {

      Map<String, String> getTargets();

      Map<String, Set<String>> getKeyMap();

      Map<String, String> getProperties();
   }

   public static class FileConfig implements Config {

      private interface Parser {

         /**
          *
          * @param line The trimed line to work with.
          * @return TRUE when line did match the requirements of the parser and was processed, else FALSE.
          */
         boolean processLine(String line);
      }

      private class CommentParser implements Parser {

         public boolean processLine(String line) {
            return line.startsWith("#");
         }
      }

      private class DefaultKeyParser implements Parser {

        public boolean processLine(String line) {
            String[] splited;
            String key;

            splited = line.split(" ");
            if (splited.length > 1) {
               key = splited[0];
               pushKeyAndTargets(key, Arrays.copyOfRange(splited, 1, splited.length));
               return true;
            }
            return false;
         }
      }

      private class PropertyParser implements Parser {

        public boolean processLine(String line) {
            String[] splited;

            splited = line.split("=", 2);
            if (splited.length > 1) {
               properties.put(splited[0].trim().toLowerCase(), splited[1].trim());
            }
            return false;
         }
      }

      private String filename;
      private Map<String, String> targets;
      private Map<String, Set<String>> keyMap;
      private Map<String, String> properties;

      private List<Parser> parsers;

      public FileConfig(String filename) throws FileNotFoundException {
         this.filename = filename;
         this.targets = new HashMap<String, String>();
         this.keyMap = new HashMap<String, Set<String>>();
         this.properties = new HashMap<String, String>();

         parsers = new ArrayList<Parser>();
         parsers.add(new CommentParser());
         parsers.add(new PropertyParser());
         parsers.add(new DefaultKeyParser());

         parseFile();
      }

      public Map<String, String> getTargets() {
         return targets;
      }

      public Map<String, Set<String>> getKeyMap() {
         return keyMap;
      }

      public Map<String, String> getProperties() {
         return properties;
      }

      private void parseFile() throws FileNotFoundException {
         BufferedReader input;
         String line;

         input = new BufferedReader(new FileReader(filename));
         try {
            while ((line = input.readLine()) != null) {
               parseLine(line);
            }
         } catch (IOException ex) {
            Logger.getLogger(EncryptionFilter.class.getName())
                    .log(Level.WARNING, ex.getLocalizedMessage());
         }
         try {
            input.close();
         } catch (IOException ex) {
            Logger.getLogger(EncryptionFilter.class.getName())
                    .log(Level.WARNING, MessageFormat.format("Error while closing file: {0}", ex.getLocalizedMessage()));
         }
      }

      private void parseLine(String line) {
         String trimed;

         trimed = line.trim();
         for (Parser parser : parsers) {
            if (parser.processLine(trimed)) {
               return;
            }
         }
      }

      private void pushKeyAndTargets(String key, String[] targets) {
         Set<String> names;

         names = keyMap.get(key);
         if (names == null) {
            names = new HashSet<String>(targets.length);
            keyMap.put(key, names);
         }
         for (String target : targets) {
            this.targets.put(target, key);
            names.add(target);
         }
      }
   }

   private static class EncryptedMessage {

      private String iv;
      private String text;

      public EncryptedMessage(String iv, String text) {
         this.iv = iv;
         this.text = text;
      }

      public String getIv() {
         return iv;
      }

      public String getText() {
         return text;
      }

      @Override
      public String toString() {
         return MessageFormat.format("{0}|{1}", iv, text);
      }
   }

   private static interface Encryptor {

      public EncryptedMessage encrypt(String message)
              throws GeneralSecurityException;

      public String decrypt(String iv, String message)
              throws GeneralSecurityException;
   }

   private static class DefaultEncryptor implements Encryptor {

      private Key key = null;

      public DefaultEncryptor(Key key) {
         this.key = key;
      }

      public DefaultEncryptor(String key)
              throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
              IOException {
         this(key, DEFAULT_CIPHER);
      }

      public DefaultEncryptor(String key, String cipher)
              throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
              IOException {
         this(Base64.decodeBase64(key.getBytes()), cipher);
      }

      public DefaultEncryptor(byte[] key, String cipher)
              throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
              IOException {
         this.key = new SecretKeySpec(key, cipher);
      }

      public EncryptedMessage encrypt(String message)
              throws GeneralSecurityException {
         Cipher cipher;
         EncryptedMessage result;

         cipher = Cipher.getInstance(key.getAlgorithm() + DEFAULT_CIPHER_MODE);
         cipher.init(Cipher.ENCRYPT_MODE, key);

         result = new EncryptedMessage(
                        new String(Base64.encodeBase64(cipher.getIV())),
                        new String(Base64.encodeBase64(cipher.doFinal(message.getBytes())))
                      );

         return result;
      }

      public String decrypt(String iv, String message)
              throws GeneralSecurityException {
         Cipher cipher;
         String result;

         cipher = Cipher.getInstance(key.getAlgorithm() + DEFAULT_CIPHER_MODE);
         cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(Base64.decodeBase64(iv.getBytes())));
         result = new String(cipher.doFinal(Base64.decodeBase64(message.getBytes())));

         return result;
      }

      @Override
      public String toString() {
         return "Encryptor for key '" + key.toString() + "'";
      }
   }

   private static class OTPEncryptor implements Encryptor {

      public static class Time {

         private int months;
         private int days;
         private int hours;
         private int minutes;
         private int seconds;

         public Time(int months, int days, int hours, int minutes, int seconds) {
            this.months = months;
            this.days = days;
            this.hours = hours;
            this.minutes = minutes;
            this.seconds = seconds;
         }

         public Time() {
            this(0, 0, 0, 0, 0);
         }

         public int getHours() {
            return hours;
         }

         public void setHours(int hour) {
            this.hours = hour;
         }

         public int getDays() {
            return days;
         }

         public void setDays(int day) {
            this.days = day;
         }

         public int getMinutes() {
            return minutes;
         }

         public void setMinutes(int minute) {
            this.minutes = minute;
         }

         public int getMonths() {
            return months;
         }

         public void setMonths(int month) {
            this.months = month;
         }

         public int getSeconds() {
            return seconds;
         }

         public void setSeconds(int second) {
            this.seconds = second;
         }

         public boolean isZero() {
            return seconds == 0 && minutes == 0 && hours == 0 && days == 0 && months == 0;
         }
      }

      private File file;
      private String algorithm;
      private int keysize;
      private Date start;
      private Time interval;
      private int bytesToSkip;

      public OTPEncryptor(File file, String algorithm, int keysize, Date start, Time interval, int bytesToSkip)
              throws FileNotFoundException, IOException {
         this.file = file;
         this.algorithm = algorithm;
         this.keysize = keysize;
         this.start = start;
         this.interval = interval;
         this.bytesToSkip = bytesToSkip;

         if ( !file.isFile() ) {
            throw new FileNotFoundException(file.getAbsolutePath());
         }
         if ( !file.canRead() ) {
            throw new IOException(MessageFormat.format("File {0} not readable.", file.getAbsolutePath()));
         }
      }

      private int getInterval(Date time) {
         int result = 0;
         Calendar calendar;
         Date marker;

         if (!start.equals(time)) {
            calendar = Calendar.getInstance();
            if (start.before(time)) {
               calendar.setTime(start);
               marker = time;
            } else {
               calendar.setTime(time);
               marker = start;
            }
            while (calendar.getTime().before(marker)) {
               calendar.add(Calendar.MONTH, interval.getMonths());
               calendar.add(Calendar.DAY_OF_MONTH, interval.getDays());
               calendar.add(Calendar.HOUR, interval.getHours());
               calendar.add(Calendar.MINUTE, interval.getMinutes());
               calendar.add(Calendar.SECOND, interval.getSeconds());
               ++result;
            }
         }

         return result;
      }

      private int lastInterval;
      private byte[] lastKey;
      private byte[] getKey(Date time) throws FileNotFoundException, IOException {
         byte[] result;
         int currentInterval;
         DataInputStream input;

         result = new byte[keysize];

         currentInterval = getInterval(time);
         if (currentInterval != lastInterval) {
            input = new DataInputStream(new FileInputStream(file));
            input.skipBytes(currentInterval * bytesToSkip);
            if (input.read(result) == -1) {
               input.close();
               throw new IOException("End of file reached. Not enuogh bytes to create a key.");
            }
            input.close();

            lastInterval = currentInterval;
            lastKey = result;
         } else {
            result = lastKey;
         }

         return result;
      }

      private Encryptor getEncryptor(Date time)
              throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
         return new DefaultEncryptor(getKey(time), algorithm);
      }

      public EncryptedMessage encrypt(String message) throws GeneralSecurityException {
         Date time;
         Encryptor encryptor;
         EncryptedMessage encryptedMessage;

         try {

            time = new Date();
            encryptor = getEncryptor(time);
            encryptedMessage = encryptor.encrypt(message);
            return new EncryptedMessage(encryptedMessage.getIv(), time.getTime() + ":" + encryptedMessage.getText());

         } catch (IOException ex) {
            throw new GeneralSecurityException(ex);
         }
      }

      public String decrypt(String iv, String message) throws GeneralSecurityException {
         String[] parts;

         try {
            parts = message.split(":", 2);
            return getEncryptor(new Date(Long.parseLong(parts[0]))).decrypt(iv, parts[1]);
         } catch (IOException ex) {
            throw new GeneralSecurityException(ex);
         }
      }

      @Override
      public String toString() {
         return "OTP Encryptor (" + file.getName() + ")";
      }
   }

   private static interface KeyParser {

      /**
       *
       * @param configString The String from the key configuration file.
       * @return an encryptor for the configString, or NULL.
       * @throws java.security.GeneralSecurityException
       * @throws java.io.IOException
       */
      public Encryptor getEncryptor(String configString)
              throws GeneralSecurityException, IOException;
   }

   private static class DefaultKeyParser implements KeyParser {

      public Encryptor getEncryptor(String configString)
              throws GeneralSecurityException, IOException {
         return new DefaultEncryptor(configString);
      }
   }

   private static class ExtendedKeyParser implements KeyParser {

      private static final Pattern pattern = Pattern.compile("^(\\S+):(\\S+)$");

      public Encryptor getEncryptor(String configString)
              throws GeneralSecurityException, IOException {
         Matcher matcher;

         matcher = pattern.matcher(configString);
         if (matcher.matches()) {
            return new DefaultEncryptor(matcher.group(2), matcher.group(1));
         }

         return null;
      }
   }

   private static class OTPKeyParser implements KeyParser {

      private static final Pattern pattern = Pattern.compile("^OTP\\((\\S+),([0-9]+),(\\S*),(\\S*),([0-9]+),([^\\(]+)\\)$", Pattern.CASE_INSENSITIVE);
      private static final Pattern intervalPattern = Pattern.compile("([0-9]+[Mdhms])");

      private SimpleDateFormat dateFormat;
      private SimpleDateFormat dateOnlyFormat;

      public OTPKeyParser() {
         dateFormat = new SimpleDateFormat("yyyyMMdd'T'kkmmssZ");
         dateOnlyFormat = new SimpleDateFormat("yyyyMMdd");
      }

      public Encryptor getEncryptor(String configString)
              throws GeneralSecurityException, IOException {
         Matcher matcher;

         matcher = pattern.matcher(configString);
         if (matcher.matches()) {
            String algorithm;
            int keysize;
            String start;
            String interval;
            String filename;
            int skipBytes;
            Date startDate;
            OTPEncryptor.Time intervalTime;

            try {

               algorithm = matcher.group(1);
               keysize = Integer.parseInt(matcher.group(2)) / 8;
               start = matcher.group(3);
               interval = matcher.group(4);
               skipBytes = Integer.parseInt(matcher.group(5));
               filename = matcher.group(6);

               try {
                  startDate = dateFormat.parse(start);
               } catch (ParseException ex) {
                  startDate = dateOnlyFormat.parse(start);
               }

               intervalTime = parseInterval(interval);

               return new OTPEncryptor(
                       new File(filename),
                       algorithm,
                       keysize,
                       startDate,
                       intervalTime,
                       skipBytes
                    );

            } catch (FileNotFoundException ex) {
               Logger.getLogger(EncryptionFilter.class.getName()).log(Level.WARNING, ex.getLocalizedMessage());
            } catch (IOException ex) {
               Logger.getLogger(EncryptionFilter.class.getName()).log(Level.WARNING, ex.getLocalizedMessage());
            } catch (ParseException ex) {
               Logger.getLogger(EncryptionFilter.class.getName()).log(Level.WARNING, ex.getLocalizedMessage());
            } catch (NumberFormatException ex) {
               Logger.getLogger(EncryptionFilter.class.getName()).log(Level.WARNING, ex.getLocalizedMessage());
            }
         }

         return null;
      }

      private OTPEncryptor.Time parseInterval(String interval) {
         OTPEncryptor.Time result;
         Matcher intervalMatcher;

         result = new OTPEncryptor.Time();

         intervalMatcher = intervalPattern.matcher(interval);
         while (intervalMatcher.find()) {
            try {
               int value = Integer.parseInt(intervalMatcher.group().substring(0, intervalMatcher.group().length() - 1));
               if (intervalMatcher.group().endsWith("M")) {
                  result.setMonths(value);
               } else if (intervalMatcher.group().endsWith("d")) {
                  result.setDays(value);
               } else if (intervalMatcher.group().endsWith("h")) {
                  result.setHours(value);
               } else if (intervalMatcher.group().endsWith("m")) {
                  result.setMinutes(value);
               } else if (intervalMatcher.group().endsWith("s")) {
                  result.setSeconds(value);
               }
            } catch (NumberFormatException ex) {
               Logger.getLogger(EncryptionFilter.class.getName()).log(Level.WARNING, ex.getLocalizedMessage());
            }
         }

         return result;
      }
   }

   public static final String DEFAULT_CIPHER = "AES";
   public static final String DEFAULT_CIPHER_MODE = "/CBC/ISO10126PADDING";
   public static final String PREFIX = "<birch>";
   public static final String DONT_ENCRYPT_PREFIX = "<plain>";

   private static final Pattern messagePattern = Pattern.compile("^:(\\S+) (PRIVMSG) (\\S+) :(.*)$");
   private static final Pattern encryptedPattern = Pattern.compile("(" + PREFIX + "\\|\\S+\\|\\S+)");

   private Pattern activeEncryptedPattern;

   private String encryptedPrefix;
   private String plainPrefix;
   private String ignorePrefix;

   /** Map of target to Enryptor. */
   private Map<String, Encryptor> targets;
   /** Map of key to target. */
   private Map<String, String> keyToTarget;

   private List<KeyParser> keyParsers;
   private Filter linebreakFilter;

   public EncryptionFilter(Filter filter, Config config) {
      super(filter);

      this.targets = new HashMap<String, Encryptor>();
      this.keyToTarget = new HashMap<String, String>();
      linebreakFilter = new LinebreakFilter(Filter.nullObject);

      keyParsers = new ArrayList();
      keyParsers.add(new OTPKeyParser());
      keyParsers.add(new ExtendedKeyParser());
      keyParsers.add(new DefaultKeyParser());

      encryptedPrefix = config.getProperties().get("prefixencrypted") == null
                           ? ""
                           : config.getProperties().get("prefixencrypted");
      plainPrefix = config.getProperties().get("prefixplain") == null
                           ? ""
                           : config.getProperties().get("prefixplain");
      ignorePrefix = config.getProperties().get("ignoreprefix") == null
                           ? ""
                           : config.getProperties().get("ignoreprefix");

      if ( ignorePrefix.matches(".*[^\\\\]*(\\\\\\\\)*$") ) {
         activeEncryptedPattern = Pattern.compile(
                 MessageFormat.format("(?:{0})?{1}", ignorePrefix, encryptedPattern.pattern()));
      } else {
         activeEncryptedPattern = encryptedPattern;
      }

      Logger logger = Logger.getLogger(EncryptionFilter.class.getName());
      logger.finer("encryptedPrefix: " + encryptedPrefix);
      logger.finer("plainPrefix: " + plainPrefix);
      logger.finer("ignoreprefix: " + ignorePrefix);
      logger.finer("activeEncryptedPattern: " + activeEncryptedPattern.pattern());

      String key;
      Encryptor encryptor;
      Map<String, String> configTargets;
      configTargets = config.getTargets();

      for (String target : configTargets.keySet()) {
         try {

            key = configTargets.get(target);
            if (key != null) {
               encryptor = getEncryptor(key);
               if (encryptor != null) {
                  this.targets.put(target, encryptor);
                  this.keyToTarget.put(key, target);
               }
            }

         } catch (GeneralSecurityException ex) {
            Logger.getLogger(EncryptionFilter.class.getName()).log(Level.WARNING, ex.getLocalizedMessage());
         } catch (IOException ex) {
            Logger.getLogger(EncryptionFilter.class.getName()).log(Level.WARNING, ex.getLocalizedMessage());
         }
      }
   }

   private Encryptor getEncryptor(String keyFromConfig)
           throws GeneralSecurityException, IOException {
      Encryptor result = null;

      for (KeyParser keyParser : keyParsers) {
         result = keyParser.getEncryptor(keyFromConfig);
         if (result != null) {
            break;
         }
      }

      return result;
   }

   protected String doSend(String message) {
      StringBuilder result;
      String[] parts;

      parts = message.split(" ", 3);
      if ("PRIVMSG".equals(parts[0])) {
         if (parts[2].startsWith(":" + DONT_ENCRYPT_PREFIX)) {
            return parts[0] + " " + parts[1] + " :" + parts[2].substring(DONT_ENCRYPT_PREFIX.length() + 1);
         } else {
            if (inEncryptionList(parts[1])) {
               result = new StringBuilder();

               result.append(parts[0]);
               result.append(" ");
               result.append(parts[1]);
               result.append(" :");
               result.append(encrypt(parts[1], parts[2].substring(1)));

               return linebreakFilter.send(result.toString());
            }
         }
      }

      return message;
   }

   protected String doReceive(String message) {
      StringBuilder result;
      String target;
      Matcher messageMatcher;
      Matcher encryptedMatcher;

      message = linebreakFilter.receive(message);
      if (message.length() == 0) {
         return message;
      }

      messageMatcher = messagePattern.matcher(message);
      if ( messageMatcher.matches() ) {
         target = messageMatcher.group(3).startsWith("#")
                     ? messageMatcher.group(3)
                     : messageMatcher.group(1).substring(1, messageMatcher.group(1).indexOf('!'));
         if (inEncryptionList(target)) {
            result = new StringBuilder();
            result.append(":");
            result.append(messageMatcher.group(1));
            result.append(" ");
            result.append(messageMatcher.group(2));
            result.append(" ");
            result.append(messageMatcher.group(3));
            result.append(" :");

            encryptedMatcher = activeEncryptedPattern.matcher(messageMatcher.group(4));
            if (encryptedMatcher.matches()) {
               result.append( messageMatcher.group(4).substring(0, encryptedMatcher.start(encryptedMatcher.groupCount())) );
               result.append( decrypt(target, encryptedMatcher.group(encryptedMatcher.groupCount())) );
            } else {
               result.append(plainPrefix);
               result.append(messageMatcher.group(4));
            }

            return result.toString();
         }
      }

      return message;
   }

   private boolean inEncryptionList(String test) {
      for (String target : targets.keySet()) {
         if (test.matches(target)) {
            return true;
         }
      }
      return false;
   }

   private String encrypt(String target, String message) {
      String result;

      result = message;
      try {

         result = PREFIX + "|" + targets.get(target).encrypt(message);

      } catch (GeneralSecurityException ex) {
         Logger.getLogger(EncryptionFilter.class.getName()).log(Level.WARNING, ex.getLocalizedMessage());
      }

      return result;
   }

   private String decrypt(String target, String message) {
      String[] splited;
      Encryptor encryptor;

      splited = message.split("\\|", 3);
      try {

         if (splited.length == 3) {
            encryptor = targets.get(target);
            if (encryptor == null) {
               Logger.getLogger(EncryptionFilter.class.getName())
                       .log(Level.INFO, "No Encryptor for " + target);
            } else {
               return encryptedPrefix + encryptor.decrypt(splited[1], splited[2]);
            }
         }

      } catch (GeneralSecurityException ex) {
         Logger.getLogger(EncryptionFilter.class.getName()).log(Level.WARNING, ex.getLocalizedMessage());
      }

      return message;
   }

   @Override
   public String toString() {
      return EncryptionFilter.class.getName();
   }
}
