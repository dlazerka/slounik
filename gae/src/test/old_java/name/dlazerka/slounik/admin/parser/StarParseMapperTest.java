package name.dlazerka.slounik.admin.parser;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

public class StarParseMapperTest extends EasyMockSupport {
	StarParseMapper patient;

	@Before
	public void setUp() {
		patient = new StarParseMapper();
	}

	@Test
	public void testSimple() throws Exception {
		List<String> res = patient.mapInternal("<b>встрепать</b> — ускудлаціць, узлахмаціць");
		assertEquals("ускудлаціць", res.get(0));
		assertEquals("узлахмаціць", res.get(1));
	}

	@Test
	public void testУх() throws IOException {
		String str = "<b>ух</b> <i>межд.</i><br>"
				+ "<b>1.</b> <i>(при выражении восхищения, удивления)</i> ух<br>"
				+ "<b>2.</b> <i>(при выражении чувства усталости и других чувств)</i> ух<br><b>ух, как жарко</b> ух, як горача<br>"
				+ "<b>3.</b> <i>(при выражении резкого низкого звука от удара, выстрела)</i> ух<br>"
				+ "<b>ух! - раздался глухой удар</b> ух! - раздаўся глухі ўдар";
		List<String> res = patient.mapInternal(str);
		assertArrayEquals(new String[] { "ух" }, res.toArray(new String[0]));
	}

	@Test
	public void testТрое() throws IOException {
		String str = "<b>трое</b> <i>(с сущ. муж.)</i> тры, <i>род.</i> трох; "
				+ "<i>(с сущ. муж. и жен., вместе взятыми, с сущ. общего рода, с сущ., употребляющимися только во мн., с сущ., обозначающими детей и детёнышей, с личными мест. мн.)</i> трое, <i>род.</i> траіх<br>"
				+ "<b>трое товарищей</b> тры таварышы<br>"
				+ "<b>их было трое - двое мужчин и одна женщина</b> іх было трое - два мужчыны і адна жанчына<br>"
				+ "<b>у них трое детей</b> у іх трое дзяцей<br>"
				+ "<b>трое котят</b> трое кацянят<br>"
				+ "<b>трое суток</b> трое сутак";
		List<String> res = patient.mapInternal(str);
		assertTrue(res.contains("тры"));
		assertTrue(res.contains("трое"));
		assertTrue(res.size() == 2);
	}

	@Test
	public void testУрок() throws IOException {
		String str = "<b>урок</b> <i>в разн. знач.</i> урок, -ка <i>муж.</i><br>"
				+ "<b>урок белорусского языка</b> урок беларускай мовы<br>"
				+ "<b>это послужит ему уроком</b> гэта паслужыць (будзе) яму урокам<br>"
				+ "<b>брать уроки (чего-либо у кого-либо)</b> браць урокі (чаго-небудзь у каго-небудзь)<br>"
				+ "<b>давать уроки (где-либо, кому-либо)</b> даваць урокі (дзе-небудзь, каму-небудзь)";
		List<String> res = patient.mapInternal(str);
		assertTrue(res.contains("урок"));
		assertTrue(res.size() == 1);
	}

	@Test
	public void testТудаСюда() throws IOException {
		String str = "<b>туда-сюда</b> <i>разг.</i><br><b>1.</b> <i>нареч. (в ту и другую сторону)</i> туды-сюды<br>"
				+ "<b>посмотреть туда-сюда</b> паглядзець туды-сюды<br>"
				+ "<b>2.</b> <i>безл. в знач. сказ. (сойдёт, ничего)</i> сяк-так, сюды-туды<br>"
				+ "<b>это ещё туда-сюда</b> гэта яшчэ сяк-так (сюды-туды)";
		List<String> res = patient.mapInternal(str);
		assertTrue(res.contains("туды-сюды"));
		assertTrue(res.contains("сяк-так"));
		assertTrue(res.contains("сюды-туды"));
		assertTrue(res.size() == 3);
	}

	@Test
	public void testУчитель() throws IOException {
		String str = "<b>учитель</b> <i>в разн. знач.</i> настаўнік, -ка <i>муж.</i><br>"
				+ "<b>народный учитель</b> народны настаўнік<br>"
				+ "<b>присвоить почётное звание «Народный учитель Республики Беларусь»</b> прысвоіць ганаровае званне «Народны настаўнік Рэспублікі Беларусь»";
		List<String> res = patient.mapInternal(str);
		assertTrue(res.contains("настаўнік"));
		assertTrue(res.size() == 1);
	}

	@Test
	public void testФантазия() throws IOException {
		String str = "<b>фантазия</b> <i>в разн. знач.</i> фантазія, -зіі <i>жен.</i><br>"
				+ "<b>творческая фантазия</b> творчая фантазія<br>"
				+ "<b>предаваться фантазиям</b> фантазіраваць<br>"
				+ "<b>всё это одна фантазия</b> усё гэта адна фантазія<br>"
				+ "<b>«Фантазия» Шумана</b> <i>муз.</i> «Фантазія» Шумана";
		List<String> res = patient.mapInternal(str);
		assertTrue(res.contains("фантазія"));
		assertTrue(res.size() == 1);
	}

	@Test
	public void testФаренгейт() throws IOException {
		String str = "<b>фаренгейт</b>: <b>термометр фаренгейта</b> тэрмометр Фарэнгейта<br>"
				+ "<b>100° по фаренгейту</b> 100° па Фарэнгейту";
		List<String> res = patient.mapInternal(str);
		assertTrue(res.size() == 0);
	}

	@Test
	public void testЭвклидов() throws IOException {
		String str = "<b>евклидов</b> <i>см.</i> <a href=\"эвклидов\">эвклидов</a>";
		List<String> res = patient.mapInternal(str);
		assertTrue(res.size() == 0);
	}

	@Test
	public void testПорошок() throws IOException {
		String str = "<b>порош<u>о</u>к</b> - параш<u>о</u>к, -шк<u>у</u>" +
			"\n<br><b>порош<u>о</u>к алм<u>а</u>зный</b> - параш<u>о</u>к алм<u>а</u>зны" +
			"\n<br><b>порош<u>о</u>к водораспылённый</b> - параш<u>о</u>к водарасп<u>ы</u>лены" +
			"\n<br><b>порош<u>о</u>к высококоэрцит<u>и</u>вный магн<u>и</u>тный</b> - параш<u>о</u>к высокакаэрцыт<u>ы</u>ўны магн<u>і</u>тны" +
			"\n<br><b>порош<u>о</u>к газораспылённый</b> - параш<u>о</u>к газарасп<u>ы</u>лены" +
			"\n<br><b>порош<u>о</u>к карб<u>и</u>да тит<u>а</u>на</b> - параш<u>о</u>к карб<u>і</u>ду тыт<u>а</u>ну" +
			"\n<br><b>порош<u>о</u>к лег<u>и</u>рованный</b> - параш<u>о</u>к лег<u>і</u>раваны" +
			"\n<br><b>порош<u>о</u>к м<u>а</u>гний-ц<u>и</u>нковый ферр<u>и</u>товый</b> - параш<u>о</u>к м<u>а</u>гній-ц<u>ы</u>нкавы фер<u>ы</u>тавы" +
			"\n<br><b>порош<u>о</u>к магн<u>и</u>тный</b> - параш<u>о</u>к магн<u>і</u>тны" +
			"\n<br><b>порош<u>о</u>к магнитом<u>я</u>гкий ферр<u>и</u>товый</b> - параш<u>о</u>к магнітам<u>я</u>ккі фер<u>ы</u>тавы" +
			"\n<br><b>порош<u>о</u>к м<u>е</u>лкий</b> - параш<u>о</u>к др<u>о</u>бны" +
			"\n<br><b>порош<u>о</u>к мелкодисп<u>е</u>рсный</b> - параш<u>о</u>к дробнадысп<u>е</u>рсны" +
			"\n<br><b>порош<u>о</u>к металл<u>и</u>ческий</b> - параш<u>о</u>к метал<u>і</u>чны" +
			"\n<br><b>порош<u>о</u>к металл<u>и</u>ческого тит<u>а</u>на</b> - параш<u>о</u>к метал<u>і</u>чнага тыт<u>а</u>ну" +
			"\n<br><b>порош<u>о</u>к окс<u>и</u>дный</b> - параш<u>о</u>к акс<u>і</u>дны" +
			"\n<br><b>порош<u>о</u>к предвар<u>и</u>тельно лег<u>и</u>рованный</b> - параш<u>о</u>к папяр<u>э</u>дне лег<u>і</u>раваны" +
			"\n<br><b>порош<u>о</u>к разм<u>е</u>ром 5-6 мкм</b> - параш<u>о</u>к пам<u>е</u>рам 5-6 мкм" +
			"\n<br><b>порош<u>о</u>к специ<u>а</u>льный</b> - параш<u>о</u>к спецы<u>я</u>льны" +
			"\n<br><b>порош<u>о</u>к субмикр<u>о</u>нный</b> - параш<u>о</u>к субмікр<u>о</u>нны" +
			"\n<br><b>порош<u>о</u>к ультрадисп<u>е</u>рсный</b> - параш<u>о</u>к ультрадысп<u>е</u>рсны" +
			"\n<br><b>порош<u>о</u>к ферр<u>и</u>товый</b> - параш<u>о</u>к фер<u>ы</u>тавы" +
			"\n<br><b>порош<u>о</u>к ферроабраз<u>и</u>вный</b> - параш<u>о</u>к фераабраз<u>і</u>ўны";
		List<String> res = patient.mapInternal(str);
		assertTrue(res.contains("парашок"));
		assertTrue(res.size() == 1);
	}

	@Test
	public void testКривая() throws IOException {
		String str = "<b>крив<u>а</u>я</b> — крыв<u>а</u>я, -в<u>о</u>й" +
			"<br><b>крив<u>а</u>я <i>p</i>-ад<u>и</u>ческая</b> — крыв<u>а</u>я <i>p</i>-ад<u>ы</u>чная" +
			"<br><b>крив<u>а</u>я бриллю<u>э</u>новская</b> — крыв<u>а</u>я брылю<u>э</u>наўская" +
			"<br><b>крив<u>а</u>я градуир<u>о</u>вочная</b> — крыв<u>а</u>я градуір<u>о</u>вачная" +
			"<br><b>крив<u>а</u>я затух<u>а</u>ния фосфоресц<u>е</u>нции</b> — крыв<u>а</u>я затух<u>а</u>ння фасфарэсц<u>э</u>нцыі" +
			"<br><b>крив<u>а</u>я произв<u>о</u>льная</b> — крыв<u>а</u>я адв<u>о</u>льная" +
			"<br><b>крив<u>а</u>я простр<u>а</u>нственная</b> — крыв<u>а</u>я праст<u>о</u>равая" +
			"<br><b>крив<u>а</u>я распредел<u>е</u>ния Г<u>а</u>усса</b> — крыв<u>а</u>я размеркав<u>а</u>ння Г<u>а</u>ўса" +
			"<br><b>крив<u>а</u>я с хор<u>о</u>шей ред<u>у</u>кцией</b> — крыв<u>а</u>я з д<u>о</u>брай рэд<u>у</u>кцыяй" +
			"<br><b>крив<u>а</u>я уд<u>а</u>рного нагруж<u>е</u>ния</b> — крыв<u>а</u>я ўд<u>а</u>рнага нагруж<u>э</u>ння" +
			"<br><b>крив<u>а</u>я эллипт<u>и</u>ческая</b> — крыв<u>а</u>я эліпт<u>ы</u>чная";
		List<String> res = patient.mapInternal(str);
		assertTrue(res.contains("крывая"));
		assertTrue(res.size() == 1);
	}

	@Test
	public void testШШ() throws IOException {
		String str = "<b>ш-ш</b> <i>межд. разг.</i> ш-ш; ша; ціха<br><b>ш-ш, слушайте!</b> ш-ш, слухайце!";
		List<String> res = patient.mapInternal(str);
		assertTrue(res.contains("ш-ш"));
		assertTrue(res.contains("ша"));
		assertTrue(res.contains("ціха"));
		assertTrue(res.size() == 3);
	}

	@Test
	public void testВетвь() throws IOException {
		String str = "<b>ветвь</b> — 1) галіна (<i>дрэва</i>); 2) галіна (<i>навукі</i>); 3) <i>ж.-д.</i> ветка";
		List<String> res = patient.mapInternal(str);
		assertTrue(res.contains("галіна"));
		assertTrue(res.contains("ветка"));
		assertTrue(res.size() == 2);
	}

	@Test
	public void testЫх() throws IOException {
		String str = "<b>ых</b>, <i>междомет.</i><br><b>1.</b> межд. негодования, - эх. Нсл. 722; Ксл.<br><i>Ых, які ты нягодны!</i> Лятоўшчына Куз.";
		List<String> res = patient.mapInternal(str);
//		assertTrue(res.contains("эх"));
//		assertTrue(res.size() == 1);
		assertTrue(res.isEmpty());
	}


}
