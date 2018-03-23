package cpabe;
import it.unisa.dia.gas.jpbc.Element;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import cpabe.policy.LangPolicy;
import bswabe.Bswabe;
import bswabe.BswabeCph;
import bswabe.BswabeCphKey;
import bswabe.BswabeElementBoolean;
import bswabe.BswabeMsk;
import bswabe.BswabePrv;
import bswabe.BswabePub;
import bswabe.SerializeUtils;

public class Cpabe {

	/**
	 * @param args
	 * @author Junwei Wang(wakemecn@gmail.com)
	 */

	public void setup(String pubfile, String mskfile) throws IOException,
			ClassNotFoundException {
		byte[] pub_byte, msk_byte;
		BswabePub pub = new BswabePub();
		BswabeMsk msk = new BswabeMsk();
		Bswabe.setup(pub, msk);

		/* store BswabePub into mskfile */
		pub_byte = SerializeUtils.serializeBswabePub(pub);
		Common.spitFile(pubfile, pub_byte);

		/* store BswabeMsk into mskfile */
		msk_byte = SerializeUtils.serializeBswabeMsk(msk);
		Common.spitFile(mskfile, msk_byte);
	}

	public void keygen(String pubfile, String prvfile, String mskfile,
			String attr_str) throws NoSuchAlgorithmException, IOException {
		BswabePub pub;
		BswabeMsk msk;
		byte[] pub_byte, msk_byte, prv_byte;

		/* get BswabePub from pubfile */
		pub_byte = Common.suckFile(pubfile);
		pub = SerializeUtils.unserializeBswabePub(pub_byte);

		/* get BswabeMsk from mskfile */
		msk_byte = Common.suckFile(mskfile);
		msk = SerializeUtils.unserializeBswabeMsk(pub, msk_byte);

		String[] attr_arr = LangPolicy.parseAttribute(attr_str);
		BswabePrv prv = Bswabe.keygen(pub, msk, attr_arr);

		/* store BswabePrv into prvfile */
		prv_byte = SerializeUtils.serializeBswabePrv(prv);
		Common.spitFile(prvfile, prv_byte);
	}

	public void enc(String pubfile, String policy, String inputfile,
			String encfile) throws Exception {
		BswabePub pub;
		BswabeCph cph;
		BswabeCphKey keyCph;
		byte[] plt;
		byte[] cphBuf;
		byte[] aesBuf;
		byte[] pub_byte;
		Element m;

		/* get BswabePub from pubfile */
		pub_byte = Common.suckFile(pubfile);
		pub = SerializeUtils.unserializeBswabePub(pub_byte);

		keyCph = Bswabe.enc(pub, policy);
		cph = keyCph.cph;
		m = keyCph.key;
		System.err.println("m = " + m.toString());

		if (cph == null) {
			System.out.println("Error happed in enc");
			System.exit(0);
		}

		cphBuf = SerializeUtils.bswabeCphSerialize(cph);

		/* read file to encrypted */
		plt = Common.suckFile(inputfile);
		aesBuf = AESCoder.encrypt(m.toBytes(), plt);
		// PrintArr("element: ", m.toBytes());
		Common.writeCpabeFile(encfile, cphBuf, aesBuf);
	}

	public void dec(String pubfile, String prvfile, String encfile,
			String decfile) throws Exception {
		byte[] aesBuf, cphBuf;
		byte[] plt;
		byte[] prv_byte;
		byte[] pub_byte;
		byte[][] tmp;
		BswabeCph cph;
		BswabePrv prv;
		BswabePub pub;

		/* get BswabePub from pubfile */
		pub_byte = Common.suckFile(pubfile);
		pub = SerializeUtils.unserializeBswabePub(pub_byte);

		/* read ciphertext */
		tmp = Common.readCpabeFile(encfile);
		aesBuf = tmp[0];
		cphBuf = tmp[1];
		cph = SerializeUtils.bswabeCphUnserialize(pub, cphBuf);

		/* get BswabePrv form prvfile */
		prv_byte = Common.suckFile(prvfile);
		prv = SerializeUtils.unserializeBswabePrv(pub, prv_byte);

		BswabeElementBoolean beb = Bswabe.dec(pub, prv, cph);
		System.err.println("e = " + beb.e.toString());
		if (beb.b) {
			plt = AESCoder.decrypt(beb.e.toBytes(), aesBuf);
			Common.spitFile(decfile, plt);
		} else {
			System.exit(0);
		}
	}

/****************************/
/** MODIFICATION FUNCTIONS **/
/****************************/
	
	public byte[][] setup() throws IOException, ClassNotFoundException {
		byte[][] keys = new byte[2][];
		byte[] pub_byte, msk_byte;
		BswabePub pub = new BswabePub();
		BswabeMsk msk = new BswabeMsk();
		Bswabe.setup(pub, msk);

		/* store BswabePub into mskfile */
		pub_byte = SerializeUtils.serializeBswabePub(pub);
		
		/* store BswabeMsk into mskfile */
		msk_byte = SerializeUtils.serializeBswabeMsk(msk);
		
		keys[0] = pub_byte;
		keys[1] = msk_byte;
		
		return keys;
	}
	
	public byte[] keygen(byte[] pub_key, byte[] msk_key, String attr_str) throws NoSuchAlgorithmException, IOException {
		BswabePub pub;
		BswabeMsk msk;
		byte[] prv_byte;

		/* get BswabePub from pubfile */
		pub = SerializeUtils.unserializeBswabePub(pub_key);

		/* get BswabeMsk from mskfile */
		msk = SerializeUtils.unserializeBswabeMsk(pub, msk_key);

		String[] attr_arr = LangPolicy.parseAttribute(attr_str);
		BswabePrv prv = Bswabe.keygen(pub, msk, attr_arr);

		/* store BswabePrv into prvfile */
		prv_byte = SerializeUtils.serializeBswabePrv(prv);
		
		return prv_byte;
	}
	
	public byte[][] enc(byte[] pub_key, String policy, byte[] input) {
		try {
			byte[][] enc_input = new byte[2][];
			BswabePub pub;
			BswabeCph cph;
			BswabeCphKey keyCph;
			byte[] cphBuf;
			byte[] aesBuf;
			Element m;
	
			/* get BswabePub from pubfile */
			pub = SerializeUtils.unserializeBswabePub(pub_key);
	
			
			keyCph = Bswabe.enc(pub, policy);
			cph = keyCph.cph;
			m = keyCph.key;
			if (cph == null) {
				System.out.println("Error happed in enc");
				//System.exit(0);
				throw new Exception("Error encrypting data");
			}
	
			cphBuf = SerializeUtils.bswabeCphSerialize(cph);
			aesBuf = AESCoder.encrypt(m.toBytes(), input);
			enc_input[0] = cphBuf;
			enc_input[1] = aesBuf;
			
			return enc_input;
		} catch (Exception e) {
			return null;
		}
	}

	public byte[] dec(byte[] pub_key, byte[] cpabe_key, byte[][] input) {
		try {
			byte[] aesBuf, cphBuf;
			byte[] plt = null;
			BswabeCph cph;
			BswabePrv prv;
			BswabePub pub;
	
			/* get BswabePub from pubfile */
			pub = SerializeUtils.unserializeBswabePub(pub_key);
	
			/* read ciphertext */
			cphBuf = input[0];
			aesBuf = input[1];
	
			cph = SerializeUtils.bswabeCphUnserialize(pub, cphBuf);
	
			/* get BswabePrv form cpabe_key */
			prv = SerializeUtils.unserializeBswabePrv(pub, cpabe_key);
	
			BswabeElementBoolean beb = Bswabe.dec(pub, prv, cph);
			if (beb.b) {
				plt = AESCoder.decrypt(beb.e.toBytes(), aesBuf);
			} else {
				//System.exit(0);
				throw new Exception("Error decrypting data");
			}
			
			return plt;
		} catch (Exception e) {
			return null;
		}
	}

/****************************/
/** MODIFICATION FUNCTIONS **/
/****************************/
	
}
