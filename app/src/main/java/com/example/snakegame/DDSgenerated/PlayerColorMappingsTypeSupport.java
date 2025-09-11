package com.example.snakegame.DDSgenerated;

import com.zrdds.infrastructure.*;
import com.zrdds.topic.TypeSupport;
import com.zrdds.publication.DataWriter;
import com.zrdds.subscription.DataReader;
import java.io.UnsupportedEncodingException;

public class PlayerColorMappingsTypeSupport extends TypeSupport {
    private String type_name = "PlayerColorMappings";
    private static TypeCodeImpl s_typeCode = null;
    private static PlayerColorMappingsTypeSupport m_instance = new PlayerColorMappingsTypeSupport();

    private final byte[] tmp_byte_obj = new byte[1];
    private final char[] tmp_char_obj = new char[1];
    private final short[] tmp_short_obj = new short[1];
    private final int[] tmp_int_obj = new int[1];
    private final long[] tmp_long_obj = new long[1];
    private final float[] tmp_float_obj = new float[1];
    private final double[] tmp_double_obj = new double[1];
    private final boolean[] tmp_boolean_obj = new boolean[1];

    
    private PlayerColorMappingsTypeSupport(){}

    
    public static TypeSupport get_instance() { return m_instance; }

    public Object create_sampleI() {
        PlayerColorMappings sample = new PlayerColorMappings();
        return sample;
    }

    public void destroy_sampleI(Object sample) {

    }

    public int copy_sampleI(Object dst,Object src) {
        PlayerColorMappings PlayerColorMappingsDst = (PlayerColorMappings)dst;
        PlayerColorMappings PlayerColorMappingsSrc = (PlayerColorMappings)src;
        PlayerColorMappingsDst.copy(PlayerColorMappingsSrc);
        return 1;
    }

    public int print_sample(Object _sample) {
        if (_sample == null){
            System.out.println("NULL");
            return -1;
        }
        PlayerColorMappings sample = (PlayerColorMappings)_sample;
        if (sample.room_id != null){
            System.out.println("sample.room_id:" + sample.room_id);
        }
        else{
            System.out.println("sample.room_id: null");
        }
        int mapsTmpLen = sample.maps.length();
        System.out.println("sample.maps.length():" +mapsTmpLen);
        for (int i = 0; i < mapsTmpLen; ++i){
            PlayerColorMappingTypeSupport.get_instance().print_sample(sample.maps.get_at(i));
        }
        return 0;
    }

    public String get_type_name(){
        return this.type_name;
    }

    public int get_max_sizeI(){
        return 67584;
    }

    public int get_max_key_sizeI(){
        return 67584;
    }

    public boolean has_keyI(){
        return false;
    }

    public String get_keyhashI(Object sample, long cdr){
        return "-1";
    }

    public DataReader create_data_reader() {return new PlayerColorMappingsDataReader();}

    public DataWriter create_data_writer() {return new PlayerColorMappingsDataWriter();}

    public TypeCode get_inner_typecode(){
        TypeCode userTypeCode = get_typecode();
        if (userTypeCode == null) return null;
        return userTypeCode.get_impl();
    }

    public int get_sizeI(Object _sample,long cdr, int offset) throws UnsupportedEncodingException {
        int initialAlignment = offset;
        PlayerColorMappings sample = (PlayerColorMappings)_sample;
        offset += CDRSerializer.get_string_size(sample.room_id == null ? 0 : sample.room_id.getBytes().length, offset);

        offset += CDRSerializer.get_untype_size(4, offset);
        int mapsLen = sample.maps.length();
        if (mapsLen != 0){
            for (int i = 0; i < mapsLen; ++i){
                PlayerColorMapping curEle = sample.maps.get_at(i);
                offset += PlayerColorMappingTypeSupport.get_instance().get_sizeI(curEle, cdr, offset);
            }
        }

        return offset - initialAlignment;
    }

    public int serializeI(Object _sample ,long cdr) {
         PlayerColorMappings sample = (PlayerColorMappings) _sample;

        if (!CDRSerializer.put_string(cdr, sample.room_id, sample.room_id == null ? 0 : sample.room_id.length())){
            System.out.println("serialize sample.room_id failed.");
            return -2;
        }

        if (!CDRSerializer.put_int(cdr, sample.maps.length())){
            System.out.println("serialize length of sample.maps failed.");
            return -2;
        }
        for (int i = 0; i < sample.maps.length(); ++i){
            if (PlayerColorMappingTypeSupport.get_instance().serializeI(sample.maps.get_at(i),cdr) < 0){
                System.out.println("serialize sample.mapsfailed.");
                return -2;
            }
        }

        return 0;
    }

    synchronized public int deserializeI(Object _sample, long cdr){
        PlayerColorMappings sample = (PlayerColorMappings) _sample;
        sample.room_id = CDRDeserializer.get_string(cdr);
        if(sample.room_id ==null){
            System.out.println("deserialize member sample.room_id failed.");
            return -3;
        }

        if (!CDRDeserializer.get_int_array(cdr, tmp_int_obj, 1)){
            System.out.println("deserialize length of sample.maps failed.");
            return -2;
        }
        if (!sample.maps.ensure_length(tmp_int_obj[0], tmp_int_obj[0])){
            System.out.println("Set maxiumum member sample.maps failed.");
            return -3;
        }
        PlayerColorMapping tmpmaps = new PlayerColorMapping();
        for (int i = 0; i < sample.maps.length(); ++i){
            if (PlayerColorMappingTypeSupport.get_instance().deserializeI(tmpmaps, cdr) < 0){
                System.out.println("deserialize sample.maps failed.");
                return -2;
            }
            sample.maps.set_at(i, tmpmaps);
        }

        return 0;
    }

    public int get_key_sizeI(Object _sample,long cdr,int offset)throws UnsupportedEncodingException {
        int initialAlignment = offset;
        PlayerColorMappings sample = (PlayerColorMappings)_sample;
        offset += get_sizeI(sample, cdr, offset);
        return offset - initialAlignment;
    }

    public int serialize_keyI(Object _sample, long cdr){
        PlayerColorMappings sample = (PlayerColorMappings)_sample;
        return 0;
    }

    public int deserialize_keyI(Object _sample, long cdr) {
        PlayerColorMappings sample = (PlayerColorMappings)_sample;
        return 0;
    }

    public TypeCode get_typecode(){
        if (s_typeCode != null) {
            return s_typeCode;
        }
        TypeCodeFactory factory = TypeCodeFactory.get_instance();

        s_typeCode = factory.create_struct_TC("Supplement.PlayerColorMappings");
        if (s_typeCode == null){
            System.out.println("create struct PlayerColorMappings typecode failed.");
            return s_typeCode;
        }
        int ret = 0;
        TypeCodeImpl memberTc = new TypeCodeImpl();
        TypeCodeImpl eleTc = new TypeCodeImpl();

        memberTc = factory.create_string_TC(255);
        if (memberTc == null){
            System.out.println("Get Member room_id TypeCode failed.");
            factory.delete_TC(s_typeCode);
            s_typeCode = null;
            return null;
        }
        ret = s_typeCode.add_member_to_struct(
            0,
            0,
            "room_id",
            memberTc,
            false,
            false);
        factory.delete_TC(memberTc);
        if (ret < 0)
        {
            factory.delete_TC(s_typeCode);
            s_typeCode = null;
            return null;
        }

        memberTc = (TypeCodeImpl)PlayerColorMappingTypeSupport.get_instance().get_typecode();
        if (memberTc != null)
        {
            memberTc = factory.create_sequence_TC(255, memberTc);
        }
        if (memberTc == null){
            System.out.println("Get Member maps TypeCode failed.");
            factory.delete_TC(s_typeCode);
            s_typeCode = null;
            return null;
        }
        ret = s_typeCode.add_member_to_struct(
            1,
            1,
            "maps",
            memberTc,
            false,
            false);
        factory.delete_TC(memberTc);
        if (ret < 0)
        {
            factory.delete_TC(s_typeCode);
            s_typeCode = null;
            return null;
        }

        return s_typeCode;
    }

}