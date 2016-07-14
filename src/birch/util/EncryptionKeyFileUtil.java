/*
 * EncryptionKeyFileUtil.java
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
package birch.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.*;

import java.security.*;

import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author Beselius
 */
public class EncryptionKeyFileUtil {

   public static String createKey(String cipher, int keysize)
           throws InvalidKeyException, IllegalBlockSizeException,
           NoSuchAlgorithmException, NoSuchPaddingException {
      KeyGenerator keyGenerator;
      Key key;

      keyGenerator = KeyGenerator.getInstance(cipher);
      keyGenerator.init(keysize);
      key = keyGenerator.generateKey();

      return new String(Base64.encodeBase64(key.getEncoded()));
   }

   public static void main(String[] args) {
      try {

         String cipher = "AES";
         int keysize = 128;

         if (args.length > 0) {
            cipher = args[0];
         }
         if (args.length > 1) {
            keysize = Integer.parseInt(args[1]);
         }

         System.out.println("Cipher algorithm " + cipher + " used to create a " + keysize + "bit key:");
         System.out.println(createKey(cipher, keysize));

      } catch (Throwable ex) {
         Logger.getLogger(EncryptionKeyFileUtil.class.getName()).log(Level.SEVERE, null, ex);
      }
   }
}
