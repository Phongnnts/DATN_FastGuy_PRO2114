import assert from 'node:assert/strict';
import { execFile } from 'node:child_process';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { promisify } from 'node:util';
import test from 'node:test';

const execFileAsync = promisify(execFile);
const contractPath = new URL('../../openapi/fastguy.yaml', import.meta.url);
const redoclyPath = new URL('../node_modules/@redocly/cli/bin/cli.js', import.meta.url);

async function parseContract() {
  const directory = await mkdtemp(join(tmpdir(), 'fastguy-openapi-'));
  const outputPath = join(directory, 'fastguy.json');
  try {
    await execFileAsync(process.execPath, [redoclyPath.pathname.slice(1), 'bundle', contractPath.pathname.slice(1), '--output', outputPath, '--ext', 'json']);
    return JSON.parse(await readFile(outputPath, 'utf8'));
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
}

test('OpenAPI contracts product-scoped review write and reads', async () => {
  const contract = await parseContract();
  const schemas = contract.components.schemas;
  const create = schemas.ReviewCreateRequest;
  const orderData = schemas.ReviewByOrderData;
  const customerItem = schemas.CustomerReviewItem;
  const createPath = contract.paths['/reviews'].post;
  const productPath = contract.paths['/reviews/product/{productId}'];
  const productId = productPath.get.parameters.find((parameter) => parameter.$ref === '#/components/parameters/ProductId');
  const parameters = Object.fromEntries(productPath.get.parameters.filter((parameter) => parameter.name).map((parameter) => [parameter.name, parameter]));

  assert.deepEqual(create.required.sort(), ['orderId', 'productId', 'rating']);
  assert.equal(create.additionalProperties, false);
  assert.equal(create.properties.orderId.minimum, 1);
  assert.equal(create.properties.productId.minimum, 1);
  assert.equal(create.properties.rating.type, 'integer');
  assert.equal(create.properties.rating.minimum, 1);
  assert.equal(create.properties.rating.maximum, 5);
  assert.deepEqual(create.properties.comment.type, ['string', 'null']);
  assert.equal(create.properties.comment.maxLength, 1000);
  assert.equal(create.properties.homepageConsent.default, false);
  assert.equal(createPath.responses['404'].$ref, '#/components/responses/NotFound');
  assert.equal(contract.components.responses.NotFound.description, 'Resource not found');
  assert.equal(contract.components.responses.NotFound.content['application/json'].schema.$ref, '#/components/schemas/ErrorResponse');
  assert.deepEqual(orderData.required, ['orderId', 'reviews']);
  assert.deepEqual(customerItem.required, ['reviewId', 'productId', 'rating', 'comment', 'createdAt']);
  assert.equal(customerItem.additionalProperties, false);
  assert.deepEqual(productPath.get.security, []);
  assert.equal(productId.$ref, '#/components/parameters/ProductId');
  assert.equal(contract.components.parameters.ProductId.schema.minimum, 1);
  assert.equal(parameters.page.schema.default, 1);
  assert.equal(parameters.page.schema.minimum, 1);
  assert.equal(parameters.size.schema.default, 10);
  assert.equal(parameters.size.schema.minimum, 1);
  assert.equal(parameters.size.schema.maximum, 50);
  assert.deepEqual(Object.keys(productPath.get.responses).sort(), ['200', '400', '404']);
});

test('OpenAPI strictly allowlists public reviews and aggregate page data', async () => {
  const contract = await parseContract();
  const schemas = contract.components.schemas;
  const publicItem = schemas.PublicReviewItem;
  const page = schemas.ProductReviewPage;
  const distribution = schemas.RatingDistribution;
  const forbidden = ['userId', 'orderId', 'avatarUrl', 'homepageConsent', 'featured', 'updatedAt'];

  assert.equal(publicItem.additionalProperties, false);
  assert.deepEqual(Object.keys(publicItem.properties).sort(), ['comment', 'createdAt', 'productId', 'rating', 'reviewId', 'userName']);
  assert.deepEqual(publicItem.required.sort(), ['comment', 'createdAt', 'productId', 'rating', 'reviewId', 'userName']);
  for (const field of forbidden) assert.equal(field in publicItem.properties, false);
  assert.deepEqual(publicItem.properties.comment.type, ['string', 'null']);
  assert.deepEqual(page.required.sort(), ['averageRating', 'items', 'page', 'ratingDistribution', 'reviewCount', 'size', 'total']);
  assert.equal(page.additionalProperties, false);
  assert.equal(page.properties.averageRating.type, 'number');
  assert.equal(page.properties.averageRating.minimum, 0);
  assert.equal(page.properties.averageRating.maximum, 5);
  assert.equal(page.properties.averageRating.description, 'Rounded to one decimal place. When there are no reviews, averageRating is 0.0.');
  assert.equal(page.properties.reviewCount.minimum, 0);
  assert.equal(page.properties.reviewCount.description, 'Total number of reviews for the product. reviewCount equals total and is 0 when there are no reviews.');
  assert.equal(page.properties.total.minimum, 0);
  assert.equal(page.properties.total.description, 'Total number of reviews for the product across all pages. total equals reviewCount.');
  assert.equal(page.properties.page.minimum, 1);
  assert.equal(page.properties.size.minimum, 1);
  assert.equal(page.properties.size.maximum, 50);
  assert.deepEqual(distribution.required.sort(), ['1', '2', '3', '4', '5']);
  assert.equal(distribution.additionalProperties, false);
  assert.equal(distribution.description, 'Counts for ratings 1 through 5. The sum of all five counts equals reviewCount and total; all counts are 0 when there are no reviews.');
  for (const rating of distribution.required) {
    assert.equal(distribution.properties[rating].type, 'integer');
    assert.equal(distribution.properties[rating].minimum, 0);
  }
});

test('OpenAPI contracts unpaged and paged product runtime shapes plus exact detail fields', async () => {
  const contract = await parseContract();
  const operation = contract.paths['/products'].get;
  const parameters = Object.fromEntries(operation.parameters.map(parameter => [parameter.name, parameter]));
  const response = operation.responses['200'].content['application/json'].schema;
  const detail = contract.components.schemas.ProductDetail;

  assert.deepEqual(response.oneOf.map(schema => schema.$ref).sort(), [
    '#/components/schemas/ProductListResponse',
    '#/components/schemas/ProductPageResponse',
  ]);
  assert.equal(parameters.page.schema.minimum, 0);
  assert.equal(parameters.size.schema.minimum, 1);
  assert.equal(parameters.size.schema.maximum, 48);
  assert.match(parameters.page.description, /page and size are both required/);
  assert.match(parameters.size.description, /page and size are both required/);
  assert.equal(contract.components.schemas.ProductPageData.additionalProperties, false);
  assert.deepEqual(contract.components.schemas.ProductPageData.required, ['items', 'page', 'size', 'totalItems', 'totalPages']);
  assert.equal(detail.additionalProperties, false);
  assert.deepEqual(detail.required.sort(), [...contract.components.schemas.ProductSummary.required, 'galleryImages'].sort());
  assert.equal(detail.properties.galleryImages.type, 'array');
  assert.equal('combo' in detail.properties, false);
  assert.equal(contract.components.schemas.ProductDetailResponse.properties.data.$ref, '#/components/schemas/ProductDetail');
});

test('OpenAPI requires public product rating summaries without changing featured contracts', async () => {
  const contract = await parseContract();
  const schemas = contract.components.schemas;
  const product = schemas.ProductSummary;
  const featuredProperties = Object.keys(schemas.FeaturedReview.properties).sort();

  assert.equal(product.required.includes('averageRating'), true);
  assert.equal(product.required.includes('reviewCount'), true);
  assert.equal(product.properties.averageRating.type, 'number');
  assert.equal(product.properties.averageRating.minimum, 0);
  assert.equal(product.properties.averageRating.maximum, 5);
  assert.equal(product.properties.reviewCount.type, 'integer');
  assert.equal(product.properties.reviewCount.minimum, 0);
  assert.deepEqual(featuredProperties, ['avatarUrl', 'comment', 'createdAt', 'rating', 'reviewId', 'userName']);
});
