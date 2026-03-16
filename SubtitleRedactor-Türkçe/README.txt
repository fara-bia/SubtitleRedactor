⚠️ Önemli: Programın çalışması için gereken AI modelleri (yaklaşık 5 GB) ek olarak indirmeniz gerekmektedir. Modelleri [Bu linkten: "https://drive.google.com/drive/folders/1cHR25XjXTTQFPMr0Ueokjrt3upnDgZsQ"] indirin ve içindeki ai klasörünü projenin ana dizinine yapıştırın.

# SubtitleRedactor

SubtitleRedactor, AI destekli, CPU ve GPU (CUDA) hızlandırma seçenekleri sunan temel seviyede bir altyazı imla düzenleme aracıdır.

Programı kendi bilgisayarınızda çalıştırmak için gereksinimler:
1. Java Development Kit (JDK 21)
2. Eclipse IDE for Java Developers

Kurulum adımları:
1. ZIP dosyasını çıkartın. (Bunu okuduğunuza göre muhtemelen çıkartmışsınızdır.)
2. Eclipse'i açın.
3. Sol üst menüden File > Import seçeneğine tıklayın.
4. Açılan pencerede General > Existing Projects into Workspace seçeneğini seçip Next deyin.
5. "Select root directory" kısmındaki Browse butonuna tıklayın.
6. Projects kısmında projenin tikli olduğuna emin olun ve Finish butonuna basın.

Çalıştırma adımları:
1. Eclipse'in sol tarafındaki Package Explorer panelinden `SubtitleRedactor` projesini genişletin.
2. Sırasıyla src > subtitleRedactor klasörlerini açın.
3. Main.java dosyasına sağ tıklayın.
4. Run As > Java Application seçeneğine tıklayın. (Ya da Main.java üzerine çift tıklayıp Eclipse'in yukarı menüsünden Run butonunu kullanabilirsiniz.)

Tebrikler! SubtitleRedactor arayüzü karşınıza gelecektir. Artık `.ass` dosyanızı seçip, CPU veya GPU (CUDA) tercihini belirleyerek işleminizi başlatabilirsiniz.

---
Ek Not: IDE üzerinden çalıştırmak en stabil olacaktır. Eğer JRE olmayan ortamlarda çalışabilen başarılı bir build yapabilirseniz bana da haber verebilirsiniz.