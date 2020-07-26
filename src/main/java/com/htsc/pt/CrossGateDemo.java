package com.htsc.pt;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CrossGateDemo {

    final static int HEADLEN = 40;

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("hello world!");

        //read map graph from file
        //ImgInfoHead head = new ImgInfoHead();
        Map<Integer, ImgInfoHead> imgInfoHeadMap = new HashMap<Integer, ImgInfoHead>();
        Map<Integer, ImgData> imgDataMap = new HashMap<Integer, ImgData>();

        //read *Info*.bin file
        {
            FileInputStream fileInputStream = new FileInputStream(
                    new File("/home/evan/IdeaProjects/CrossGateDemo/src/main/resources/GraphicInfo_20.bin"));

            while (true) {
                byte[] bytes;
                bytes = new byte[HEADLEN];
                if (fileInputStream.read(bytes) == -1) {
                    System.out.println("info file read end.");
                    break;
                }

                ImgInfoHead head = new ImgInfoHead();
                head.init(bytes);
                System.out.println("id:" + head.id
                        + ",addr:" + head.addr
                        + ",len:" + head.len
                        + ",xOffset:" + head.xOffset
                        + ",yOffset:" + head.yOffset
                        + ",width:" + head.width
                        + ",height:" + head.height
                        + ",tileId:" + head.tileId);

                if(head.tileId != 0) {
                    imgInfoHeadMap.put(head.id, head);
                }
            }


            fileInputStream.close();
        }

        //read *.bin file
        {
            FileInputStream fileInputStream = new FileInputStream(
                    new File("/home/evan/IdeaProjects/CrossGateDemo/src/main/resources/Graphic_20.bin"));
            DataInputStream in = new DataInputStream(new BufferedInputStream(fileInputStream));
            RandomAccessFile randomAccessFile = new RandomAccessFile(
                    new File("/home/evan/IdeaProjects/CrossGateDemo/src/main/resources/Graphic_20.bin"),
                    "r");
            Iterator<Integer> keyIter = imgInfoHeadMap.keySet().iterator();

            while (keyIter.hasNext()) {
                Integer id = keyIter.next();
                byte[] bytes;
                bytes = new byte[imgInfoHeadMap.get(id).len];
                randomAccessFile.seek(imgInfoHeadMap.get(id).addr);
                if (randomAccessFile.read(bytes) == -1) {
                    System.out.println("data file read end.");
                    break;
                }

                ImgData data = new ImgData();
                data.init(bytes);
                System.out.println("id:" + id
                        + ",tileid:" + imgInfoHeadMap.get(id).tileId
                        + ",len:" + data.len
                        + ",ver:" + (int)data.cVer
                        + ",width:" + data.width
                        + ",height:" + data.height);

                imgDataMap.put(id, data);
            }


            randomAccessFile.close();
            in.close();
        }

        //read cgp file
        FileInputStream fileInputStream = new FileInputStream(
                new File("/home/evan/IdeaProjects/CrossGateDemo/src/main/resources/palet_08.cgp"));
        byte[] _uMapCgp = new byte[804];;
        while (true) {
            if (fileInputStream.read(_uMapCgp, 16*3, 708) == -1) {
                System.out.println("cgp file read end.");
                break;
            }
        }

        draw d = new draw();
        d.setVisible(true);

        //decode img data
        Iterator<Integer> iterator = imgDataMap.keySet().iterator();
        System.out.println("len:" + imgDataMap.keySet().size());
        while(iterator.hasNext()){

            Integer id = iterator.next();
            System.out.println("id:" + id);
            if(id != 12050) continue;

            ImgData data = imgDataMap.get(id);

            if(data.cVer == 0b00000001 || data.cVer == 0b00000011){
                int len = decodeImgData(data.data, data.data.length);

                if (len == -1){
                    continue;
                }

                byte[] _imgPixel = new byte[len];
                System.arraycopy(_imgData, 0, _imgPixel, 0, len);

                d.update(_imgPixel, data.width, data.height, _uMapCgp);
                d.paint(d.getGraphics());

                Thread.sleep(1000);
            }
        }
        System.out.println("end!" );
    }

    public static class draw extends JFrame {

        int w;
        int h;
        byte[] b;
        byte[] _uMapCgp;

        private void update(byte[] b, int w, int h, byte[] _uMapCgp)
        {
            this.w = w;
            this.h = h;
            this.b = b;
            this._uMapCgp = _uMapCgp;
            setSize(500,300);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
        }
        public void paint(Graphics graphics)
        {
            graphics.drawRect(100,100, w, h);
            int m = 0;
            for (int i = 0; i < h; i++){
                for (int j = 0; j < w; j++){
                    Point p = new Point(i, j);
                    int idx = (h - m / w - 1) * w + m % w;
                    int index = b[idx]& 0xff;

                    int r,g,b;
                    r = _uMapCgp[index * 3 + 2] & 0xff;
                    g = _uMapCgp[index * 3 + 1] & 0xff;
                    b = _uMapCgp[index * 3] & 0xff;

                    graphics.setColor(new Color(r, g, b));
                    graphics.drawRect(100 + j,100 + i, 1, 1);
                    m++;
                }
            }

        }

    }

    static byte[] _imgData = new byte[1024 * 1024 + 256 * 3];
    private static int decodeImgData(byte[] p, int len)
    {
        // 图片解密 Run-Length压缩
        int iPos = 0;
        int idx = 0;
        while (iPos < len)
        {
            switch (p[iPos] & 0xF0)
            {
                case 0x00:
                {
                    // 0x0n 第二个字节c，代表连续n个字符
                    int count = p[iPos] & 0x0F;
                    ++iPos;
                    for (int i = 0; i < count; ++i)
                        System.arraycopy(p, iPos++, _imgData, idx++, 1);
                        //_imgData[idx++] = p[iPos++];
                }
                break;
                case 0x10:
                {
                    // 0x1n 第二个字节x，第三个字节c，代表n*0x100+x个字符
                    int count = (p[iPos] & 0x0F) * 0x100 + p[iPos + 1];
                    iPos += 2;
                    for (int i = 0; i < count; ++i)
                        System.arraycopy(p, iPos++, _imgData, idx++, 1);
                        //_imgData[idx++] = p[iPos++];
                }
                break;
                case 0x20:
                {
                    // 0x2n 第二个字节x，第三个字节y，第四个字节c，代表n*0x10000+x*0x100+y个字符
                    int count = (p[iPos] & 0x0F) * 0x10000 + p[iPos + 1] * 0x100 + p[iPos + 2];
                    iPos += 3;
                    for (int i = 0; i < count; ++i)
                        System.arraycopy(p, iPos++, _imgData, idx++, 1);
                        //_imgData[idx++] = p[iPos++];
                }
                break;
                case 0x80:
                {
                    // 0x8n 第二个字节X，代表连续n个X
                    int count = p[iPos] & 0x0F;
                    for (int i = 0; i < count; ++i)
                        System.arraycopy(p, iPos + 1, _imgData, idx++, 1);
                        //_imgData[idx++] = p[iPos + 1];
                    iPos += 2;
                }
                break;
                case 0x90:
                {
                    // 0x9n 第二个字节X，第三个字节m，代表连续n*0x100+m个X
                    int count = (p[iPos] & 0x0F) * 0x100 + p[iPos + 2];
                    for (int i = 0; i < count; ++i)
                        System.arraycopy(p, iPos + 1, _imgData, idx++, 1);
                        //_imgData[idx++] = p[iPos + 1];
                    iPos += 3;
                }
                break;
                case 0xa0:
                {
                    // 0xan 第二个字节X，第三个字节m，第四个字节z，代表连续n*0x10000+m*0x100+z个X
                    int count = (p[iPos] & 0x0F) * 0x10000 + p[iPos + 2] * 0x100 + p[iPos + 3];
                    for (int i = 0; i < count; ++i)
                        System.arraycopy(p, iPos + 1, _imgData, idx++, 1);
                        //_imgData[idx++] = p[iPos + 1];
                    iPos += 4;
                }
                break;
                case 0xc0:
                {
                    // 0xcn 同0x8n，只不过填充的是背景色
                    int count = p[iPos] & 0x0F;
                    for (int i = 0; i < count; ++i)
                        _imgData[idx++] = 0;
                    iPos += 1;
                }
                break;
                case 0xd0:
                {
                    // 0xdn 同0x9n，只不过填充的是背景色
                    int count = (p[iPos] & 0x0F) * 0x100 + p[iPos + 1];
                    for (int i = 0; i < count; ++i)
                        _imgData[idx++] = 0;
                    iPos += 2;
                }
                break;
                case 0xe0:
                {
                    int count = (p[iPos] & 0x0F) * 0x10000 + p[iPos + 1] * 0x100 + p[iPos + 2];
                    for (int i = 0; i < count; ++i)
                        _imgData[idx++] = 0;
                    iPos += 3;
                }
                break;
                default:
                    return -1;
            }
        }

        return idx;
    }

     private static class ImgInfoHead {
        int id;
        int addr;
        int len;
        int xOffset;
        int yOffset;
        int width;
        int height;
        char tileEast;
        char tileSouth;
        char flag;
        char unKnow[] = new char[5];
        int tileId;

        private void init(byte[] b){
            this.id = ((b[0] & 0xFF) << 0) | ((b[1] & 0xFF) << 8) |
                    ((b[2] & 0xFF) << 16) | ((b[3] & 0xFF) << 24);
            this.addr = ((b[4] & 0xFF) << 0) | ((b[5] & 0xFF) << 8) |
                    ((b[6] & 0xFF) <<  16) | ((b[7] & 0xFF) <<  24);
            this.len = (((b[8] & 0xFF) << 0) | ((b[9] & 0xFF) << 8) |
                    ((b[10] & 0xFF) <<  16) | ((b[11] & 0xFF) <<  24));
            this.xOffset = ((b[12] & 0xFF) << 0) | ((b[13] & 0xFF) << 8) |
                    ((b[14] & 0xFF) <<  16) | ((b[15] & 0xFF) <<  24) ;
            this.yOffset = ((b[16] & 0xFF) << 0) | ((b[17] & 0xFF) << 8) |
                    ((b[18] & 0xFF) <<  16) | ((b[19] & 0xFF) <<  24);
            this.width = ((b[20] & 0xFF) << 0) | ((b[21] & 0xFF) << 8) |
                    ((b[22] & 0xFF) <<  16) | ((b[23] & 0xFF) <<  24);
            this.height = ((b[24] & 0xFF) << 0) | ((b[25] & 0xFF) << 8) |
                    ((b[26] & 0xFF) <<  16) | ((b[27] & 0xFF) <<  24);
            this.tileEast = (char) (b[28] & 0xFF);
            this.tileSouth = (char) (b[29] & 0xFF);
            this.flag = (char) (b[30] & 0xFF);
            this.tileId = ((b[36] & 0xFF) << 0) | ((b[37] & 0xFF) << 8) |
                    ((b[38] & 0xFF) <<  16) | ((b[39] & 0xFF) <<  24);
        }
    }

    private static class ImgData {
        char cName[] = new char[2];
        char cVer;
        char cUnknow;
        int width;
        int height;
        int len;
        byte data[];


        public void init(byte[] b) {
            data = new byte[b.length - 16];

            this.cName[0] = (char) (b[1] & 0xFF);
            this.cName[1] = (char) (b[0] & 0xFF);
            this.cVer = (char) (b[2] & 0xFF);
            this.cUnknow = (char) (b[3] & 0xFF);
            this.width = ((b[4] & 0xFF) << 0) | ((b[5] & 0xFF) << 8) |
                    ((b[6] & 0xFF) <<  16) | ((b[7] & 0xFF) <<  24);
            this.height = (((b[8] & 0xFF) << 0) | ((b[9] & 0xFF) << 8) |
                    ((b[10] & 0xFF) <<  16) | ((b[11] & 0xFF) <<  24));
            this.len = ((b[12] & 0xFF) << 0) | ((b[13] & 0xFF) << 8) |
                    ((b[14] & 0xFF) <<  16) | ((b[15] & 0xFF) <<  24) ;
            System.arraycopy(b, 16, this.data, 0, b.length - 16);
        }
    }

    // 游戏指定的调色板0-15 BGR
    private char g_c0_15[] = {
            0x00, 0x00, 0x00,
            0x80, 0x00, 0x00,
            0x00, 0x80, 0x00,
            0x80, 0x80, 0x00,
            0x00, 0x00, 0x80,
            0x80, 0x00, 0x80,
            0x00, 0x80, 0x80,
            0xc0, 0xc0, 0xc0,
            0xc0, 0xdc, 0xc0,
            0xa6, 0xca, 0xf0,
            0xde, 0x00, 0x00,
            0xff, 0x5f, 0x00,
            0xff, 0xff, 0xa0,
            0x00, 0x5f, 0xd2,
            0x50, 0xd2, 0xff,
            0x28, 0xe1, 0x28,
    };
    // 游戏指定的调色板240-255 BGR
    private char g_c240_245[] = {
            0xf5, 0xc3, 0x96,
            0x1e, 0xa0, 0x5f,
            0xc3, 0x7d, 0x46,
            0x9b, 0x55, 0x1e,
            0x46, 0x41, 0x37,
            0x28, 0x23, 0x1e,
            0xff, 0xfb, 0xf0,
            0x3a, 0x6e, 0x5a,
            0x80, 0x80, 0x80,
            0xff, 0x00, 0x00,
            0x00, 0xff, 0x00,
            0xff, 0xff, 0x00,
            0x00, 0x00, 0xff,
            0xff, 0x80, 0xff,
            0x00, 0xff, 0xff,
            0xff, 0xff, 0xff,
    };
}
